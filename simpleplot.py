import matplotlib.pyplot as plt

dtnlink = [25,78,99,99,99]
link = [20,45,64,80,99]
x = [0.2,0.4,0.6,0.8,1.0]
fontsize = 16
font = {
    'family': 'normal',
    'weight': 'bold',
    'size': fontsize
}
plt.title('Effect of pDetection in a 2 hour simulation', fontsize=fontsize)
plt.rc('font', **font)
plt.tick_params(labelsize=fontsize)
plt.xlabel('pDetection', fontsize=fontsize)
plt.ylabel('Messages Transferred', fontsize=fontsize)
plt.plot(x,dtnlink,label='DtnLink',linewidth=3.0)
plt.plot(x,link,label='Link',linewidth=3.0)
plt.legend()
plt.show()