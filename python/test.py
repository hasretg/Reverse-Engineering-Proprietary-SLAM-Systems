from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import matplotlib.pyplot as plt
import random


pt_cloud = []
with open("pt_cloud_experiment.asc") as file:
    next(file)
    next(file)
    for row in file:
        pt_cloud.append(row.strip().split(" "))

pt_cloud = np.array(pt_cloud, dtype=np.float64)
pt_cloud = pt_cloud[random.sample(range(1, pt_cloud.shape[0]), 200000), :]
#col = np.array(pt_cloud[:, 3:6], dtype="U10")
fig = plt.figure()
ax = fig.gca(projection='3d')


s = ax.scatter(pt_cloud[:, 0], pt_cloud[:, 1], pt_cloud[:, 2], c=pt_cloud[:, 3:6], s=2)

plt.show()
print(pt_cloud.shape)

