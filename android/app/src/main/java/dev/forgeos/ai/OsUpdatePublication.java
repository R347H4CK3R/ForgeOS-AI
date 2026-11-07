package dev.forgeos.ai;

/** Failed disk commits may still mutate memory; caller retains both valid files. */
final class OsUpdatePublication {
 static final class Record {
  final String file,json,signature;final long highest;
  Record(String f,String j,String s,long h){file=f;json=j;signature=s;highest=h;}
 }
 interface Store {Record read();boolean write(Record record);}
 static boolean replace(Store store,Record replacement){
  Record previous=store.read();
  if(store.write(replacement))return true;
  store.write(previous); // Restore memory even if the durable rollback also fails.
  return false;
 }
}
