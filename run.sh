rm -r sootOutput
mkdir sootOutput
find sootInput -name "*.java" -exec javac -cp sootInput/ -d sootOutput/ {} \;
cp sootInput/jce.jar sootOutput/jce.jar
cp sootInput/rt.jar sootOutput/rt.jar

mvn clean package
cp target/PointerAnalysis-1.0-SNAPSHOT.jar analyzer.jar

res="---------- Evaluation Result Summary ----------"
for class in Hello FieldSensitivity; do
    rm result.txt
    java -jar analyzer.jar sootOutput test.$class
    if [ -f result.txt ]
    then
        cp result.txt result-$class.txt
        diff -y -s result-$class.txt correctResults/$class.txt
        res=$(printf "%s\n%20s: %s\n" "$res" $class "$(python evaluate.py result-$class.txt correctResults/$class.txt)")
    else
        res=$(printf "%s\n%20s: Crashed\n" "$res" $class)
    fi
done
echo "$res"