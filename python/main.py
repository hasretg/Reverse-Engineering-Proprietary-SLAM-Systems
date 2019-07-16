import csv
import matplotlib.pyplot as plt
from sklearn.preprocessing import normalize
from mpl_toolkits.mplot3d import axes3d
import numpy as np
from pyquaternion import Quaternion
import math
from collections import defaultdict
import os
DIR_NAME = "slam_with_poster_corr/"
GROUND_TRUTH_FILE = "ground_truth_room_markers.txt"
MARKER_ID = [3, 4, 6, 7, 8, 9, 10, 11, 12]
marker_relation = {'1': 1, '3': 2, '4': 3, '6': 4, '7': 5, '8': 6, '9': 7, '10': 8, '11': 9, '12': 10, '13': 11}
MARKER_ID_PLOT = [2, 3, 4, 5, 6, 7, 8, 9, 10]
gt_dict = {}


def main():
    with open(GROUND_TRUTH_FILE) as file:
        next(file)
        for line in file:
            fields = line.split(",")
            gt_dict[fields[0]] = [fields[1], fields[2], fields[3]]

    all_data = np.empty(shape=(len(MARKER_ID), len(next(os.walk(DIR_NAME))[2]), 3))
    all_data.fill(np.nan)
    counter_files = 0
    for file in os.listdir(DIR_NAME):

        coords, quats, timestamp = [], [], []
        m_dict = defaultdict(list)  # List of dictionaries with the markers and its information (e.g. pose)
        print(file)
        ###
        # In this section, all the data needed for the processing is extracted from the text-file with all the ArCore SLAM
        # information saved in the android application
        ###
        with open(DIR_NAME + file) as csv_file:
            csv_reader = csv.reader(csv_file, delimiter=',')
            next(csv_reader)
            line_count = 0
            for row in csv_reader:
                coords.append(row[7:10])
                quats.append(row[10:14])
                timestamp.append(row[0])
                line_count = line_count + 1

                nr_of_markers = (len(row)-15)//10  # Determine number of markers detected in a frame
                for marker in range(nr_of_markers):
                    m_dict[row[15 + 10*marker]].append({'sizeX': float(row[16 + 10*marker]), 'sizeY': float(row[17 + 10*marker]),
                                                        'px': float(row[18 + 10*marker]), 'py': float(row[19 + 10*marker]),
                                                        'pz': float(row[20 + 10*marker]), 'qx': float(row[21 + 10*marker]),
                                                        'qy': float(row[22 + 10*marker]), 'qz': float(row[23 + 10*marker]),
                                                        'qw': float(row[24 + 10*marker])})

        print(f'{line_count} lines processed.')
        coords = np.asarray(coords, dtype=np.float)
        quats = np.asarray(quats, dtype=np.float)
        timestamp = np.asarray(timestamp, dtype=np.int)

        ###
        # In this section, we plot the information extracted from the textfile, including camera pose, marker pose
        ###
        fig = plt.figure()
        ax = fig.add_subplot(111)
        #ax.scatter3D(coords[:, 0], coords[:, 1], coords[:, 2], c=timestamp, cmap='cool', s=5)  # Plot position of each frame

        for i in range(0, len(quats), 50):
            # Plot orientation of each camera frame
            mat_euler = get_euler_rotation(quats[i, :])
            # Plot axis of camera in 3D
            #plot_orientation(mat_euler, coords[i, :], ax, scale=0.8)

        initCoord = []

        counter = 1
        is_first = True
        for elem in gt_dict:
            #ax.scatter3D(float(gt_dict[elem][0]), float(gt_dict[elem][1]), float(gt_dict[elem][2]), marker="D", c='black', s=50)

            if counter == 1:
                ax.scatter(float(gt_dict[elem][0]), float(gt_dict[elem][2]), marker="D", c='black', s=100, label='Marker ground truth')
            else:
                ax.scatter(float(gt_dict[elem][0]), float(gt_dict[elem][2]), marker="D", c='black', s=100)


            ax.text(float(gt_dict[elem][0]) + 0.2, float(gt_dict[elem][2]) - 0.2, marker_relation[elem[7: -4]], fontsize=10, color='black')
            counter = counter + 1
        for marker in m_dict:

            if marker == "marker_1.jpg":
                initCoord = [float(m_dict[marker][0]['px']), float(m_dict[marker][0]['py']), float(m_dict[marker][0]['pz'])]
            else:
                ind = MARKER_ID.index(int(marker[7:-4]))
                all_data[ind, counter_files, :] = [m_dict[marker][0]['px'] - initCoord[0],
                                                                 m_dict[marker][0]['py'] - initCoord[1],
                                                                 m_dict[marker][0]['pz'] - initCoord[2]]

            for info in m_dict[marker]:
                #ax.scatter3D(float(info['px']) - float(initCoord[0]), float(info['py']) - float(initCoord[1]), float(info['pz']) - float(initCoord[2]), c='red', s=30)
                if is_first:
                    ax.scatter(float(info['px']) - float(initCoord[0]), float(info['pz']) - float(initCoord[2]), c='red', s=30, label='Captured marker position')
                    is_first = False
                    counter = 1
                else:
                    ax.scatter(float(info['px']) - float(initCoord[0]), float(info['pz']) - float(initCoord[2]), c='red', s=30)
                ax.text(float(info['px']) - float(initCoord[0]) - 0.3, float(info['pz']) - float(initCoord[2]) + 0.3, marker_relation[marker[7:-4]], fontsize=10,
                        color='red')
                counter = counter + 1
                mat_euler = get_euler_rotation([float(info['qx']), float(info['qy']), float(info['qz']), float(info['qw'])])
                #plot_orientation(mat_euler, [float(info['px']) - float(initCoord[0]), float(info['py']) - float(initCoord[1]), float(info['pz']) - float(initCoord[2])], ax, scale=0.5)
                break

        ax.scatter(coords[:, 0] - initCoord[0], coords[:, 2] - initCoord[2], c=timestamp, cmap='cool', s=5, label='Captured camera position')  # Plot position of each frame

        ax.set_xlabel('X axis')
        ax.set_ylabel('Z axis')
        #ax.set_zlabel('Z axis')

        plt.axis('equal')
        plt.gca().invert_yaxis()
        ax.legend(loc='upper right', prop={'size':10})
        #plt.show()
        plt.gcf().set_size_inches((14.4, 9), forward=False)
        plt.title(str(DIR_NAME[: -6]) + "_" + str(counter_files+1))
        plt.savefig("all_plots/" + str(DIR_NAME[:-1]) + "_" + str(counter_files+1) + ".png")

        counter_files += 1

    plot_accuracy(all_data)


def get_euler_rotation(q):
    quad = Quaternion(q[3], q[0], q[1], q[2]).normalised
    return quad.rotation_matrix


# Checks if a matrix is a valid rotation matrix.
def is_rotation_matrix(R):
    Rt = np.transpose(R)
    should_be_identiy = np.dot(Rt, R)
    I = np.identity(3, dtype=R.dtype)
    n = np.linalg.norm(I - should_be_identiy)
    return n < 1e-6


# Calculates rotation matrix to euler angles
# The result is the same as MATLAB except the order
# of the euler angles ( x and z are swapped ).
def rotation_matrix_to_euler_angles(R):
    assert (is_rotation_matrix(R))

    sy = math.sqrt(R[0, 0] * R[0, 0] + R[1, 0] * R[1, 0])
    singular = sy < 1e-6

    if not singular:
        x = math.atan2(R[2, 1], R[2, 2])
        y = math.atan2(-R[2, 0], sy)
        z = math.atan2(R[1, 0], R[0, 0])
    else:
        x = math.atan2(-R[1, 2], R[1, 1])
        y = math.atan2(-R[2, 0], sy)
        z = 0
    return np.array([x, y, z])


# Rotates 3-D vector with the euler angle X->Y->Z
def euler_rotation(matrix, theta):
    R_x = np.array([[1, 0, 0], [0, np.cos(theta[0]), -np.sin(theta[0])], [0, np.sin(theta[0]), np.cos(theta[0])]])
    R_y = np.array([[np.cos(theta[1]), 0, np.sin(theta[1])], [0, 1, 0], [-np.sin(theta[1]), 0, np.cos(theta[1])]])
    R_z = np.array([[np.cos(theta[2]), -np.sin(theta[2]), 0], [np.sin(theta[2]), np.cos(theta[2]), 0], [0, 0, 1]])
    R_tot = np.multiply(np.multiply(R_z, R_y), R_x)
    return np.multiply(R_tot, matrix)


def plot_orientation(mat, coord, fig, scale=0.3):

    # Plot axis of camera in 3D
    fig.plot([coord[0], coord[0] + mat[0, 0] * scale], [coord[1], coord[1] + mat[1, 0] * scale], [coord[2]
             , coord[2] + mat[2, 0] * scale], c='r')
    fig.plot([coord[0], coord[0] + mat[0, 1] * scale], [coord[1], coord[1] + mat[1, 1] * scale], [coord[2]
             , coord[2] + mat[2, 1] * scale], c='g')
    fig.plot([coord[0], coord[0] + mat[0, 2] * scale], [coord[1], coord[1] + mat[1, 2] * scale], [coord[2]
             , coord[2] + mat[2, 2] * scale], c='b')


def plot_accuracy(dat):
    std_dev = np.zeros(shape=((len(MARKER_ID), 3)), dtype=np.float)
    diff_coord = np.zeros(shape=((len(MARKER_ID), 3)), dtype=np.float)
    for i in range(dat.shape[0]):
        try:
            dat_mean = np.nanmean(dat[i, :, :], axis=0)
        except:
            print("An exception occurred")

        tmp = [0, 0, 0]
        for j in range(dat.shape[1]):
            if not np.isnan(dat[i, j, :]).any():
                tmp += np.power(np.subtract(dat[i, j, :], dat_mean), 2)

        std_dev[i, :] = np.divide(np.sqrt(tmp), len(MARKER_ID)-1)
        gt_pt = gt_dict.get("marker_"+str(MARKER_ID[i])+".jpg")
        gt_pt = [float(gt_pt[0]), float(gt_pt[1]), float(gt_pt[2])]
        diff_coord[i, :] = np.abs(np.subtract(gt_pt, dat_mean))
    std_dev = np.sqrt(np.sum(np.power(std_dev, 2), axis=1))
    diff_coord = np.sqrt(np.sum(np.power(diff_coord, 2), axis=1))

    plt.figure()
    plt.errorbar(MARKER_ID_PLOT, diff_coord, std_dev, linestyle='None', marker='o', color='r')
    plt.xlabel('Marker ID')
    plt.ylabel('Point difference to ground truth in [m]')
    plt.title(DIR_NAME[:-1])
    #plt.gca().set_ylim([0, 0.6])
    plt.show()


if __name__ == '__main__':
    main()
