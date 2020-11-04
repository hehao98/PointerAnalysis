for filename in sootInput/*.java; do
  /Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/bin/javac "$filename" -d sootOutput
done
mvn clean package
java -jar target/PointerAnalysis-1.0-SNAPSHOT.jar "$@"