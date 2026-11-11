"""Risk-based maintenance & verification scheduling (Gurobi MIP) - Team 4, ProRail Data Challenge.

Inputs
  * asset_health_dataset.csv  -> tasks (every asset not 'Verified Normal'), risk score P_i
  * maint_windows.parquet     -> real night maintenance-mode windows (8 Jun - 27 Oct)
Horizon
  * 30 days, 2 Nov - 1 Dec 2026. Night windows are assumed to repeat the pattern of the
    last observed 5 weeks (28 Sep - 27 Oct) shifted by +35 days (same weekdays).
  * Daytime remote-test slots: weekdays 10:00-14:00 (the 1 Oct automatic test ran 09:58-14:09).
  * Daytime on-site slots (only for locations reachable from ground level): weekdays 08:00-16:00.
All assumptions are listed in ASSUMPTIONS and written to the output workbook.
"""
import os
import sys
import pandas as pd
import numpy as np
import gurobipy as gp
from gurobipy import GRB

from config import OUT
W = str(OUT) + '/'

# ---- scenario switches (set by s05_scenarios.py through environment variables)
SCEN = dict(access=os.environ.get('SCEN_ACCESS', 'night'),     # night | day : tech rooms & pump cellars reachable only in night windows, or also in daytime
            trph=os.environ.get('SCEN_TRPH', 'remote'),        # remote | onsite : staircase overpressure fans testable via PSI or not
            weight=os.environ.get('SCEN_WEIGHT', 'linear'),    # linear | square : objective weight P or P^2
            pen=int(os.environ.get('SCEN_PEN', '37')),
            hr=os.environ.get('SCEN_HR', 'must'))              # must | free : every risk>=20 asset that the crew can handle must be verified in the horizon          # day value of an unverified asset (horizon 30 + 7 by default)
SCEN_NAME = (f"{SCEN['access']}-{SCEN['trph']}-{SCEN['weight']}" + ('' if SCEN['pen'] == 37 else f"-pen{SCEN['pen']}")
             + ('' if SCEN['hr'] == 'must' else '-hrfree'))
TECH_ROOMS = ('TR-3O', 'PK-Z', 'PK-N', 'TRPH')
H0 = pd.Timestamp('2026-11-02')          # horizon start (Monday)
H = 30                                    # days
SHIFT = pd.Timedelta(days=35)
SETUP = 30                                # min lost per night window (access, safety briefing, hand-back)

ASSUMPTIONS = [
    ('Horizon', '30 days, 2 Nov - 1 Dec 2026'),
    ('Night windows', 'Observed maintenance-mode windows of 28 Sep - 27 Oct, shifted +35 days (same weekday); windows < 30 min dropped'),
    ('Usable window time', 'Window length minus 30 min for access and hand-back'),
    ('Remote test slots', 'Weekdays 10:00-14:00 via PSI, 240 min/day, one operator (1 Oct test ran in daytime)'),
    ('Daytime on-site slots', 'Weekdays 08:00-16:00 only for surface locations (Rijwiel); 480 min per crew member'),
    ('Access (base case)', 'Tube assets need that tube window; TR-3O / PK-Z / TRPH-xO -> east tube window; TRPH-xW -> west; PK-N -> platform window'),
    ('Remote-testable', 'Overpressure fans TRPH (same system as PK-N/PK-Z fans that are tested remotely) - to confirm in Q&A; tunnel fans'),
    ('Durations', 'Measured: tunnel fan test 10 min, overpressure test <1 min (5 min used). Assumed: drainage pump test run 30 min/2 people, fire pump test 45 min/2, valve function test 15 min/1, failed-fan inspection & repair 120 min/2 (RVO median ~100 min), detector inspection/recalibration 60 min/2'),
    ('Precedence', 'A failed tunnel fan is retested remotely on a later day than its on-site repair'),
    ('Shared crew', 'One maintenance crew per night: windows of different tubes on the same night share the same people'),
    ('Location set-up', '20 min x 2 people per distinct location visited in a window (walk-in, isolate, hand back): Buis-1W track, Buis-2O track, TR-3O, PK-Z, PK-N, each TRPH'),
    ('Objective', 'min risk-weighted verification delay = sum over ASSETS of P_i x (day the asset is verified); a failed fan is verified on the day of its remote retest (the repair task carries no weight of its own); an unverified asset counts as horizon + 7 days'),
    ('High-risk rule', 'Every asset with risk >= 20 that the crew size can handle must be verified within the 30 days (hard constraint); scenario SCEN_HR=free drops it'),
    ('Excluded', 'PA/evacuation system: already restored (~22 Oct); kept in dataset as history'),
]

# ---------------------------------------------------------------- tasks
A = pd.read_csv(W + 'asset_health_dataset.csv')
A = A[(A.status != 'Verified Normal') & (A.category != 'PA / evacuation')].copy()

def area(zone):
    if SCEN['access'] == 'day' and zone.startswith(TECH_ROOMS): return 'DAY'
    if zone.startswith('Buis-1W') or zone.endswith('W') and zone.startswith('TRPH'): return 'W'
    if zone == 'PK-N': return 'P'
    if zone.startswith('Rijwiel'): return 'DAY'
    return 'E'   # Buis-2O, TR-3O, PK-Z, TRPH-xO

tasks = []
for r in A.itertuples():
    tid = f"{r.category[:4]}|{r.zone}|{r.object}"
    if r.category == 'Overpressure fan' and SCEN['trph'] == 'onsite' and r.zone.startswith('TRPH'):
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='onsite', dur=15, crew=1, area=area(r.zone), P=r.risk_score, cat=r.category, action='on-site functional test'))
    elif r.category == 'Overpressure fan':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='remote', dur=5, crew=0, area='REMOTE', P=r.risk_score, cat=r.category, action='remote functional test'))
    elif r.category == 'Tunnel fan' and r.status == 'Verification Gap':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='remote', dur=10, crew=0, area='REMOTE', P=r.risk_score, cat=r.category, action='remote test (+ add to monthly programme)'))
    elif r.category == 'Tunnel fan':   # test failed -> repair on site, then remote retest
        tasks.append(dict(task=tid + '|repair', asset=f'{r.zone} {r.object}', kind='onsite', dur=120, crew=2, area=area(r.zone), P=r.risk_score, cat=r.category, action='inspect & repair'))
        tasks.append(dict(task=tid + '|retest', asset=f'{r.zone} {r.object}', kind='remote', dur=10, crew=0, area='REMOTE', P=r.risk_score, cat=r.category, action='remote retest', pred=tid + '|repair'))
    elif r.category == 'Drainage pump':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='onsite', dur=30, crew=2, area=area(r.zone), P=r.risk_score, cat=r.category, action='on-site test run'))
    elif r.category == 'Fire pump':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='onsite', dur=45, crew=2, area=area(r.zone), P=r.risk_score, cat=r.category, action='on-site test run'))
    elif r.category == 'Fire main valve':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='onsite', dur=15, crew=1, area=area(r.zone), P=r.risk_score, cat=r.category, action='function test'))
    elif r.category == 'Train detector':
        tasks.append(dict(task=tid, asset=f'{r.zone} {r.object}', kind='onsite', dur=60, crew=2, area=area(r.zone), P=r.risk_score, cat=r.category, action='inspect / recalibrate, release suppression'))
T = pd.DataFrame(tasks)
if 'pred' not in T: T['pred'] = np.nan
# objective weight per task: an asset counts once, on the task that completes its verification.
# repair tasks only enable the retest -> weight 0 (otherwise a failed fan's risk would be counted twice)
T['w'] = np.where(T.task.str.endswith('|repair'), 0, T.P)

# ---------------------------------------------------------------- slots
mw = pd.read_parquet(W + 'maint_windows.parquet')
mw['start'] = pd.to_datetime(mw.start).dt.tz_localize(None)
mw['end'] = pd.to_datetime(mw.end).dt.tz_localize(None)
mw = mw[(mw.start >= '2026-09-28') & (mw.start < '2026-10-28') & (mw['min'] >= 30)].copy()
mw['start'] = mw.start + SHIFT; mw['end'] = mw.end + SHIFT
amap = {'Buis-1W': 'W', 'Buis-2O': 'E', 'Perron': 'P'}
slots = []
for r in mw.itertuples():
    slots.append(dict(slot=f"N-{amap[r.zone]}-{r.start:%m%d}", area=amap[r.zone], start=r.start,
                      cap=max(0, r.min - SETUP), day=(r.start.normalize() - H0).days))
for d in range(H):
    day = H0 + pd.Timedelta(days=d)
    if day.weekday() < 5:
        slots.append(dict(slot=f"R-{day:%m%d}", area='REMOTE', start=day + pd.Timedelta(hours=10), cap=240, day=d))
        slots.append(dict(slot=f"D-{day:%m%d}", area='DAY', start=day + pd.Timedelta(hours=8), cap=480, day=d))
S = pd.DataFrame(slots)
S['night'] = np.where(S.slot.str.startswith('N-'), (S.start - pd.Timedelta(hours=12)).dt.date.astype(str), '')
T['site'] = T.asset.str.split(' ').str[0]
SETUP_LOC = 20          # minutes per location visit
SETUP_CREW = 2          # people involved in a location set-up
PEN = SCEN['pen']        # day value of an unverified asset (default H + 7)

def feasible(t, s):
    return t.area == s.area and t.dur <= s.cap

# ---------------------------------------------------------------- solvers
def solve_mip(crew, verbose=False):
    m = gp.Model('maint'); m.Params.OutputFlag = 1 if verbose else 0
    pairs = [(i, j) for i, t in T.iterrows() for j, s in S.iterrows() if feasible(t, s)]
    x = m.addVars(pairs, vtype=GRB.BINARY, name='x')
    # each task at most once
    for i in T.index:
        m.addConstr(gp.quicksum(x[i, j] for (ii, j) in pairs if ii == i) <= 1)
    # location-visit indicators y[l, j] for on-site slots
    y = {}
    for j, s in S.iterrows():
        if s.area == 'REMOTE': continue
        for l in T.loc[[i for (i, jj) in pairs if jj == j], 'site'].unique():
            y[l, j] = m.addVar(vtype=GRB.BINARY, name=f'y[{l},{j}]')
            for i in [i for (i, jj) in pairs if jj == j and T.site[i] == l]:
                m.addConstr(x[i, j] <= y[l, j])
    def load(j):   # person-minutes used in on-site slot j (work + location set-ups)
        idx = [i for (i, jj) in pairs if jj == j]
        return (gp.quicksum(T.dur[i] * T.crew[i] * x[i, j] for i in idx)
                + gp.quicksum(SETUP_LOC * SETUP_CREW * y[l, jj] for (l, jj) in y if jj == j))
    for j, s in S.iterrows():
        idx = [i for (i, jj) in pairs if jj == j]
        if not idx: continue
        if s.area == 'REMOTE':
            m.addConstr(gp.quicksum(T.dur[i] * x[i, j] for i in idx) <= s.cap)
        else:
            m.addConstr(load(j) <= crew * s.cap)
            for i in idx:
                if T.crew[i] > crew: m.addConstr(x[i, j] == 0)
    # shared crew: all windows of one night together <= crew x longest usable window that night
    for n, g in S[S.night != ''].groupby('night'):
        if len(g) > 1:
            m.addConstr(gp.quicksum(load(j) for j in g.index) <= crew * g.cap.max())
    # precedence: retest on a later day than repair (and only if repaired)
    for i, t in T[T.pred.notna()].iterrows():
        k = T.index[T.task == t.pred][0]
        rep = gp.quicksum(x[k, j] for (ii, j) in pairs if ii == k)
        m.addConstr(gp.quicksum(x[i, j] for (ii, j) in pairs if ii == i) <= rep)
        for (ii, j) in pairs:
            if ii != i: continue
            for (kk, jj) in pairs:
                if kk == k and S.day[jj] >= S.day[j]:
                    m.addConstr(x[i, j] + x[k, jj] <= 1)
    # safety rule: every high-risk asset (risk >= 20) that this crew size can handle is verified within the horizon
    if SCEN['hr'] == 'must':
        for i in T.index[(T.w >= 20)]:
            chain = [i] + ([T.index[T.task == T.pred[i]][0]] if isinstance(T.pred[i], str) else [])
            if all(T.crew[c] <= crew for c in chain):
                m.addConstr(gp.quicksum(x[i, j] for (ii, j) in pairs if ii == i) == 1, name=f'hr[{i}]')
    # risk-exposure days: verified on day d -> P*(d+1); unverified -> P*PEN
    wgt = (T.w ** 2 / 5) if SCEN['weight'] == 'square' else T.w
    obj = gp.quicksum(wgt[i] * (PEN - (S.day[j] + 1)) * x[i, j] for (i, j) in pairs)
    m.setObjective(obj, GRB.MAXIMIZE)   # = minimise sum P*PEN - obj
    m.optimize()
    sol = [(i, j) for (i, j) in pairs if x[i, j].X > 0.5]
    stats = dict(vars=m.NumVars, cons=m.NumConstrs, status=m.Status, gap=m.MIPGap if m.IsMIP else 0, obj=m.ObjVal)
    return sol, stats

def solve_greedy(crew, key='risk', batch=False):
    """Heuristic baselines. Give each task (in priority order) the earliest feasible slot with capacity left.
    key='risk': risk descending (what a planner does by hand)
    key='wspt': risk per person-minute (P / (dur x crew)), the classic weighted-shortest-processing-time rule
    batch=True: after placing a task on site, immediately add other open tasks at the same location to that slot."""
    left = {j: (S.cap[j] if S.area[j] == 'REMOTE' else crew * S.cap[j]) for j in S.index}
    night_left = {n: crew * g.cap.max() for n, g in S[S.night != ''].groupby('night')}
    visited = set(); done = {}; sol = []
    pr = T.P if key == 'risk' else T.P / (T.dur * T.crew.clip(lower=1))
    order = list(pr.sort_values(ascending=False, kind='stable').index)
    Ss = S.sort_values('start')

    def try_place(i, j):
        t, s = T.loc[i], S.loc[j]
        if i in done or t.crew > crew or not feasible(t, s): return False
        need = t.dur if t.area == 'REMOTE' else t.dur * t.crew + (0 if (t.site, j) in visited else SETUP_LOC * SETUP_CREW)
        if left[j] < need: return False
        if s.night and night_left[s.night] < need: return False
        if isinstance(t.pred, str):
            k = T.index[T.task == t.pred][0]
            if k not in done or S.day[done[k]] >= s.day: return False
        left[j] -= need
        if s.night: night_left[s.night] -= need
        if t.area != 'REMOTE': visited.add((t.site, j))
        done[i] = j; sol.append((i, j)); return True

    for i in order:
        for j in Ss.index:
            if try_place(i, j):
                if batch and T.area[i] != 'REMOTE':
                    for i2 in order:
                        if T.site[i2] == T.site[i]: try_place(i2, j)
                break
    return sol

def kpis(sol, crew, name):
    got = {i: j for i, j in sol}
    fin = T[T.w > 0]                      # one row per asset: the task that completes its verification
    hi = fin[fin.P >= 20]
    days_hi = [S.day[got[i]] + 1 for i in hi.index if i in got]
    vdays = [S.day[got[i]] + 1 for i in fin.index if i in got]
    used = sum(T.dur[i] * (T.crew[i] if T.area[i] != 'REMOTE' else 1) for i in got if S.area[got[i]] in ('W', 'E', 'P'))
    avail = sum(crew * S.cap[j] for j in S.index if S.area[j] in ('W', 'E', 'P'))
    exposure = sum(T.P[i] * ((S.day[got[i]] + 1) if i in got else PEN) for i in fin.index)
    done_exp = sum(T.P[i] * (S.day[got[i]] + 1) for i in fin.index if i in got)
    return dict(policy=name, crew=crew, risk_exposure_days=int(exposure),
                exposure_verified_part=int(done_exp), unverified_risk=int(T.P[[i for i in fin.index if i not in got]].sum()),
                tasks_done=f"{len(got)}/{len(T)}", assets_verified=f"{len(vdays)}/{len(fin)}",
                avg_day_verified=round(np.mean(vdays), 2) if vdays else None,
                risk_covered_pct=round(100 * T.P[[i for i in fin.index if i in got]].sum() / fin.P.sum(), 1),
                high_risk_assets_verified=f"{len(days_hi)}/{len(hi)}",
                avg_day_high_risk=round(np.mean(days_hi), 2) if days_hi else None,
                night_window_util_pct=round(100 * used / avail, 1) if avail else 0)

if __name__ == '__main__':
    rows, sols = [], {}
    for crew in (1, 2, 3):
        g = solve_greedy(crew); rows.append(kpis(g, crew, 'Risk-order greedy'))
        rows.append(kpis(solve_greedy(crew, key='wspt'), crew, 'WSPT greedy'))
        rows.append(kpis(solve_greedy(crew, key='risk', batch=True), crew, 'Risk-order greedy + location batching'))
        rows.append(kpis(solve_greedy(crew, key='wspt', batch=True), crew, 'WSPT greedy + location batching'))
        mip, st = solve_mip(crew); rows.append(kpis(mip, crew, 'Gurobi MIP')); sols[crew] = mip
        print(f'crew {crew}: MIP status {st["status"]}, vars {st["vars"]}, cons {st["cons"]}, gap {st["gap"]:.4f}')
    K = pd.DataFrame(rows); print(K.to_string(index=False))
    sol = sols[2]
    sched = pd.DataFrame([dict(**T.loc[i, ['asset', 'cat', 'action', 'kind', 'dur', 'crew', 'P']].to_dict(),
                               slot=S.slot[j], start=S.start[j], day=S.day[j] + 1) for i, j in sol]).sort_values('start')
    un = T[~T.index.isin([i for i, _ in sol])][['asset', 'cat', 'action', 'kind', 'dur', 'crew', 'P', 'area']]
    sfx = '' if SCEN_NAME == 'night-remote-linear' else f'_{SCEN_NAME}'
    K.insert(0, 'scenario', SCEN_NAME)
    T.to_csv(W + f'gurobi_tasks{sfx}.csv', index=False); S.to_csv(W + f'gurobi_slots{sfx}.csv', index=False)
    sched.to_csv(W + f'gurobi_schedule_crew2{sfx}.csv', index=False); K.to_csv(W + f'gurobi_kpis{sfx}.csv', index=False)
    pd.DataFrame(ASSUMPTIONS, columns=['Item', 'Assumption']).to_csv(W + 'gurobi_assumptions.csv', index=False)
    print(sched.to_string(index=False)); print('UNSCHEDULED (crew 2):'); print(un.to_string(index=False))
