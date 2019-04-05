import csv
import matplotlib.pyplot as plt
from sklearn.preprocessing import normalize
from mpl_toolkits.mplot3d import axes3d
import numpy as np
from pyquaternion import Quaternion
import math
from collections import defaultdict
import os


DIR_NAME = "pose_cloud_anchor/"
GROUND_TRUTH_FILE = DIR_NAME+"ground_truth_cloud_anchor.txt"

gt_dict = {}
with open(GROUND_TRUTH_FILE) as gt_file:
    next(gt_file)
    for line in gt_file:
        fields = line.split(",")
        gt_dict[fields[0]] = [float(fields[1]), float(fields[2]), float(fields[3])]

for file in os.listdir(DIR_NAME):
    if file.startswith("155"):
        with open(DIR_NAME+file) as f:
            next(f)
            counter = 1
            for line in f:
                fields = line.split(",")
                point = [float(fields[4]), float(fields[5]), float(fields[6])]
                count = 0
                diff = []
                for el in gt_dict[str(counter)]:
                    print(el)
                    print(point[count])
                    diff.append(el-point[count])
                    count +=1
                #diff = np.divide(gt_dict[str(counter)], point)
                print("Difference point "+fields[0]+": "+str(diff))
                counter +=1
