"""Validate and package the attributed factual station-network snapshot (no network requests)."""
import json, math
from pathlib import Path
root=Path(__file__).resolve().parents[1]
raw=(root/'data/raw/rail-networks-v1.json').read_text(encoding='utf-8')
data=json.loads(raw)
assert data['geometry']=='schematic-station-connections'
ids=set()
for line in data['lines']:
    key=(line['city'],line['id'])
    assert key not in ids
    ids.add(key)
    assert line['city'] in (0,1) and len(line['stations'])>=2
    assert line['color'].startswith('#') and len(line['color'])==7
    for stop in line['stations']:
        assert stop['name'].strip() and 35<=stop['lat']<=43 and 25<=stop['lon']<=45
        assert math.isfinite(stop['lat']) and math.isfinite(stop['lon'])
assert sum(c==0 for c,_ in ids)==5
assert sum(c==1 for c,_ in ids)==19
(root/'android-legacy/assets/rail-networks.json').write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print('PASS: 5 Ankara + 19 Istanbul rail lines, unique IDs, named stations and valid coordinates')

models=json.loads((root/'data/raw/rail-estimates-v1.json').read_text(encoding='utf-8'))
assert models['timezone']=='GMT+03:00' and models['dwellSeconds']==20
for profile in models['profiles']:
    assert all(0<t<=7200 for t in profile['tripSeconds'])
    if profile['kind']=='departures':
        assert len(profile['departures'])==2
        for direction in profile['departures']:
            assert len(direction)==3
            for day in direction:
                assert day==sorted(set(day)) and day and all(21600<=t<108000 for t in day)
    else:
        assert 60<=profile['headwaySeconds']<=3600
(root/'android-legacy/assets/rail-estimates.json').write_text(json.dumps(models,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print('PASS: attributed timetable/model profiles, two directions, day types and bounded durations')
