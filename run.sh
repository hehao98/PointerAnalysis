rm -r sootOutput
mkdir sootOutput
find sootInput -name "*.java" -exec javac -cp sootInput/ -d sootOutput/ {} \;
cp sootInput/jce.jar sootOutput/jce.jar
cp sootInput/rt.jar sootOutput/rt.jar

mvn clean package
cp target/PointerAnalysis-1.0-SNAPSHOT.jar analyzer.jar

res="---------- Evaluation Result Summary ----------"
classes=( # Basic Tests
          "Hello" "Hello2" "FieldSensitivity2" "FlowSensitivity1" "FlowSensitivity2" "Recursion"
          "ContextSensitivity1" "Function" "Function2" "StaticFieldRef" "ImplicitAllocId" "Inheritance"
          "ForLoop" "ForLoopComplex" "If" "WeakUpdate" "FieldInField"
          # Advanced Tests
          "FieldSensitivity" "PointerInHeap" "RecursionComplex" "FinalTest")
if [ "$#" -ge 1 ]
then
    classes=( "$@" )
fi
for class in "${classes[@]}"; do
    rm result.txt
    java -jar analyzer.jar sootOutput test."$class"
    if [ -f result.txt ]
    then
        cp result.txt result-"$class".txt
        diff -y -s result-"$class".txt correctResults/"$class".txt
        eval_result=$(python evaluate.py result-"$class".txt correctResults/"$class".txt)
        res=$(printf "%s\n%20s: %s\n" "$res" "$class" "$eval_result")
    else
        res=$(printf "%s\n%20s: Crashed\n" "$res" "$class")
    fi
done
echo "$res"