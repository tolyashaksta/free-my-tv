"""Exercise the actual status migration and top-button methods with an in-memory settings store."""
import os
from pathlib import Path
import re
import subprocess

repo = Path(__file__).resolve().parent.parent
source = (repo / 'app/src/io/freemytv/Prefs.java').read_text(encoding='utf-8')
out = repo / 'out/status-prefs-tests'
out.mkdir(parents=True, exist_ok=True)

def method(name):
    match = re.search(r'^    (?:private )?static [\w<>]+ ' + name + r'\(', source, re.M)
    assert match, name
    start = source.index('{', match.start())
    depth = 1
    end = start + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[match.start():end]

harness = '''import java.util.*;
class SharedPreferences {
 final Map<String,Object> values = new HashMap<>();
 boolean contains(String k) { return values.containsKey(k); }
 String getString(String k, String fallback) { return (String)values.getOrDefault(k, fallback); }
 boolean getBoolean(String k, boolean fallback) { return (Boolean)values.getOrDefault(k, fallback); }
 Editor edit() { return new Editor(); }
 class Editor {
  Editor putString(String k, String v) { values.put(k,v); return this; }
  Editor putBoolean(String k, boolean v) { values.put(k,v); return this; }
  Editor remove(String k) { values.remove(k); return this; }
  boolean commit() { return true; }
  void apply() {}
 }
}
class Prefs {
 static int version;
 static SharedPreferences store = new SharedPreferences();
 static SharedPreferences sp() { return store; }
 static void put(String key, boolean value) { store.edit().putBoolean(key,value).apply(); version++; }
 static void migrate() { migrateStatusIcons(); }
'''
harness += '\n'.join(method(name) for name in ('statusIcons', 'setStatusIcons', 'migrateStatusIcons', 'topButtons', 'setTopButtons'))
harness += '''
}
public class StatusPrefsTest {
 static int assertions;
 static void check(boolean ok) { assertions++; if (!ok) throw new AssertionError("Preference migration failed"); }
 public static void main(String[] args) {
  Prefs.migrate(); check(!Prefs.statusIcons()); check(Prefs.topButtons()==null);
  Prefs.store.edit().putString("topButtons","wifi,tv-settings,vpn,settings,recents").putBoolean("topStatusEdited",true).commit();
  Prefs.migrate(); check(Prefs.statusIcons());
  check(Prefs.topButtons().equals(Arrays.asList("tv-settings","settings","recents")));
  Prefs.setStatusIcons(false); Prefs.migrate(); check(!Prefs.statusIcons());
  Prefs.setTopButtons(Arrays.asList("settings","tv-settings"));
  check(!Prefs.store.contains("topStatusEdited"));
  check(Prefs.topButtons().equals(Arrays.asList("settings","tv-settings")));
  Prefs.store = new SharedPreferences();
  Prefs.store.edit().putString("topButtons","settings").putBoolean("topStatusEdited",true).commit();
  Prefs.migrate(); check(!Prefs.statusIcons());
  Prefs.store = new SharedPreferences();
  Prefs.store.edit().putBoolean("topStatusEdited",false).commit();
  Prefs.migrate(); check(Prefs.statusIcons());
  System.out.println("StatusPrefs: " + assertions + " assertions passed");
 }
}
'''
(out / 'StatusPrefsTest.java').write_text(harness, encoding='utf-8')
bin_dir = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else Path('')
subprocess.run([str(bin_dir / 'javac'), '-encoding', 'UTF-8', '-d', str(out), str(out / 'StatusPrefsTest.java')], check=True)
subprocess.run([str(bin_dir / 'java'), '-cp', str(out), 'StatusPrefsTest'], check=True)
