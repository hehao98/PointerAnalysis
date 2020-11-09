import sys


def get_eval_result(path):
    result = dict()
    with open(path, "r") as f:
        for line in f:
            key, values = line.split(":")
            values = list(map(int, values.split()))
            result[key] = values
    return result


res = get_eval_result(sys.argv[1])
gt = get_eval_result(sys.argv[2])
unsound = False
for k in gt.keys():
    if k not in res:
        unsound = True
        continue
    for p in gt[k]:
        if p not in res[k]:
            unsound = True
if unsound:
    print("Unsound")
else:
    print("Precision={:.4f}".format(sum(sum(gt.values(), [])) / sum(sum(res.values(), []))))