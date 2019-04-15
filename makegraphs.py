import json
import matplotlib.pyplot as plt
import os
import glob

graphPath = "graphs"
filelist = [f for f in glob.glob("results/*.json")]

dtnlink = []
reglink = []
for jsonFile in filelist:
    with open(jsonFile, 'r') as f:
        data = json.load(f)
        if (data['agentName'] == 'dtnlink'):
            dtnlink.append(data)
        else:
            reglink.append(data)

for jsonData in dtnlink:
    jsonDict = jsonData['uniqueDatagrams']
    sortedTimes = []

    for x in jsonDict:
        sortedTimes.append(jsonDict[x])
    msgCount = range(1,len(sortedTimes)+1)
    sortedTimes.sort()
    plt.plot(sortedTimes, msgCount)
    plt.show()
# print(x)
# print(y)
# plt.plot(x,y)
# plt.show()