import sqlite3,json,unittest
from pathlib import Path
root=Path(__file__).resolve().parents[1]
class CatalogTest(unittest.TestCase):
 def setUp(self):self.db=sqlite3.connect(root/'android-legacy/assets/transit-v09.sqlite')
 def tearDown(self):self.db.close()
 def test_integrity(self):
  self.assertEqual(self.db.execute('pragma integrity_check').fetchone()[0],'ok')
  for table,col,target in [('stops','source','sources'),('routes','source','sources'),('route_stops','route','routes'),('route_stops','stop','stops'),('departures','stop','stops'),('departures','route','routes')]:
   self.assertEqual(self.db.execute(f'select count(*) from {table} a left join {target} b on a.{col}=b.id where b.id is null').fetchone()[0],0)
 def test_bounds(self):
  self.assertEqual(self.db.execute('select count(*) from stops where lat<35 or lat>43 or lon<25 or lon>45').fetchone()[0],0)
  self.assertGreater(self.db.execute('select count(*) from stops where city=1').fetchone()[0],15000)
 def test_web_matches_native(self):
  for i in range(7):
   c=json.loads((root/f'web/data/{i}.json').read_text(encoding="utf-8"))
   for table in ['routes','stops']:self.assertEqual(len(c[table]),self.db.execute(f'select count(*) from {table} where city=?',(i,)).fetchone()[0])
 def test_expired_izban(self):
  self.assertEqual(self.db.execute("select end from sources where name like '%İZBAN%'").fetchone()[0],'20250101')
 def test_no_empty_routes(self):
  self.assertEqual(self.db.execute("select count(*) from routes where trim(code)='' or trim(name)=''").fetchone()[0],0)
 def test_encoding(self):
  self.assertEqual(self.db.execute("select count(*) from routes where name like '%Ã%' or name like '%Ä%'").fetchone()[0],0)
if __name__=='__main__':unittest.main(verbosity=2)
