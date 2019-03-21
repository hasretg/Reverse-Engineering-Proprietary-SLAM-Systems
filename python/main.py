import csv
import matplotlib.pyplot as plt
from sklearn.preprocessing import normalize
from mpl_toolkits.mplot3d import axes3d
import numpy as np
from pyquaternion import Quaternion
import math
from collections import defaultdict

FILE_NAME = 'poseFiles/1553159282171.txt'


def main():
    coords, quats, timestamp = [], [], []
    m_dict = defaultdict(list)  # List of dictionaries with the markers and its information (e.g. pose)

    ###
    # In this section, all the data needed for the processing is extracted from the text-file with all the ArCore SLAM
    # information saved in the android application
    ###
    with open(FILE_NAME) as csv_file:
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
                m_dict[row[15 + 10*marker]].append({'sizeX': row[16 + 10*marker], 'sizeY': row[17 + 10*marker],
                                                    'px': row[18 + 10*marker], 'py': row[19 + 10*marker],
                                                    'pz': row[20 + 10*marker], 'qx': row[21 + 10*marker],
                                                    'qy': row[22 + 10*marker], 'qz': row[23 + 10*marker],
                                                    'qw': row[24 + 10*marker]})

    print(f'{line_count} lines processed.')
    coords = np.asarray(coords, dtype=np.float)
    quats = np.asarray(quats, dtype=np.float)
    timestamp = np.asarray(timestamp, dtype=np.int)

    #Initial coordinate of the marker
    init_coord = np.array([0.75111485, -0.18368168, 0.5400902])
    # When first starting app, this is the default orientation
    init_quat = Quaternion(0.707, 0, 0, -0.707)
    # Transformation of
    img_to_cam_quat = Quaternion(-0.6854666, -0.17241786, -0.18655144, -0.68235344).normalised

    ''''' Transform position and orientation to the correct coordinate system
    for i in range(len(timestamp)):
        currQuat = Quaternion(quats[i, 3], quats[i, 0], quats[i, 1], quats[i, 2]).normalised
        finalQuat = init_quat.inverse*img_to_cam_quat.inverse*currQuat
        mat_euler = (init_quat.inverse *img_to_cam_quat.inverse).rotation_matrix
        q = init_quat.inverse*img_to_cam_quat.inverse

        rot_mat = np.ndarray((3, 3));
        rot_mat[0, 0] = 1-2*(q.y)*(q.y) - 2*(q.z)*q.z
        rot_mat[0, 1] = 2*q.x*q.y - 2*q.z*q.w
        rot_mat[0, 2] = 2*q.x*q.z + 2*q.y*q.w
        rot_mat[1, 0] = 2*q.x*q.y + 2*q.z*q.w
        rot_mat[1, 1] = 1-2*(q.x)*(q.x) - 2*(q.z)*q.z
        rot_mat[1, 2] = 2*q.y*q.z - 2*q.x*q.w
        rot_mat[2, 0] = 2*q.x*q.z - 2*q.y*q.w
        rot_mat[2, 1] = 2*q.y*q.z + 2*q.x*q.w
        rot_mat[2, 2] = 1-2*(q.x)*(q.x) - 2*(q.y)*q.y
        print(rot_mat - mat_euler)
        #print(mat_euler)


        coords[i, :] = np.matmul(mat_euler, coords[i, :] + init_coord)
        quats[i, :] = [finalQuat.x, finalQuat.y, finalQuat.z, finalQuat.w]

    '''
    ###
    # In this section, we plot the information extracted from the textfile, including camera pose, marker pose
    ###
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.scatter3D(coords[:, 0], coords[:, 1], coords[:, 2], c=timestamp, cmap='cool')  # Plot position of each frame
    for i in range(0, len(quats), 10):
        # Plot orientation of each camera frame
        mat_euler = get_euler_rotation(quats[i, :])
        # Plot axis of camera in 3D
        scale = 1

        ax.plot([coords[i, 0], coords[i, 0] - mat_euler[0, 0] * scale], [coords[i, 1], coords[i, 1] - mat_euler[1, 0]
                * scale], [coords[i, 2], coords[i, 2] - mat_euler[2, 0] * scale], c='r')
        ax.plot([coords[i, 0], coords[i, 0] - mat_euler[0, 1] * scale], [coords[i, 1], coords[i, 1] - mat_euler[1, 1]
                * scale], [coords[i, 2], coords[i, 2] - mat_euler[2, 1] * scale], c='g')
        ax.plot([coords[i, 0], coords[i, 0] - mat_euler[0, 2] * scale], [coords[i, 1], coords[i, 1] - mat_euler[1, 2]
                * scale], [coords[i, 2], coords[i, 2] - mat_euler[2, 2] * scale], c='b')
        #plot_orientation(mat_euler, coords[i, :], ax)

    for marker in m_dict:
        for info in m_dict[marker]:

            ax.scatter3D(float(info['px']), float(info['py']), float(info['pz']), marker="D", c='black', s=30)
            mat_euler = get_euler_rotation([float(info['qx']), float(info['qy']), float(info['qz']), float(info['qw'])])


            plot_orientation(mat_euler, [float(info['px']), float(info['py']), float(info['pz'])], ax, scale=2)


    max = np.max(coords, axis=0)
    min = np.min(coords, axis=0)
    ext = np.max(np.subtract(max, min)) /2
    mid = np.subtract(max, min) / 2

    # ax.set_xlim(mid[0] - ext, mid[0] + ext)
    # ax.set_ylim(mid[1] - ext, mid[1] + ext)
    # ax.set_zlim(mid[2] - ext, mid[2] + ext)
    ax.set_xlabel('X axis')
    ax.set_ylabel('Y axis')
    ax.set_zlabel('Z axis')
    plt.show()


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
    fig.plot([coord[0], coord[0] - mat[0, 0] * scale], [coord[1], coord[1] - mat[1, 0] * scale], [coord[2]
             , coord[2] - mat[2, 0] * scale], c='r')
    fig.plot([coord[0], coord[0] - mat[0, 1] * scale], [coord[1], coord[1] - mat[1, 1] * scale], [coord[2]
             , coord[2] - mat[2, 1] * scale], c='g')
    fig.plot([coord[0], coord[0] - mat[0, 2] * scale], [coord[1], coord[1] - mat[1, 2] * scale], [coord[2]
             , coord[2] - mat[2, 2] * scale], c='b')


if __name__ == '__main__':
    main()
