import csv
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import axes3d
import numpy as np
from pyquaternion import Quaternion
import math


def main():
    coords, quats, timestamp = [], [], []
    # Open txt-file with the SLAM information
    with open('poseFiles/1552401569521.txt') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        next(csv_reader)
        line_count = 0
        for row in csv_reader:
            coords.append(row[7:10])
            quats.append(row[10:14])
            timestamp.append(row[0])
            line_count = line_count + 1
        print(f'{line_count} lines processed.')
        coords = np.asarray(coords, dtype=np.float)
        quats = np.asarray(quats, dtype=np.float)
        timestamp = np.asarray(timestamp, dtype=np.int)

        x = np.array(coords[:, 0])
        y = np.array(coords[:, 1])
        z = np.array(coords[:, 2])

        unit_ax = np.eye(3)

        fig = plt.figure()
        ax = fig.add_subplot(111, projection='3d')
        for i in range(0, len(quats), 10):

            # Euler rotation of each axis
            q = Quaternion(quats[i, 1], quats[i, 2], quats[i, 3], quats[i, 0]).normalised
            euler = rotation_matrix_to_euler_angles(q.rotation_matrix)
            x_rot, y_rot, z_rot = euler_rotation(unit_ax, euler)

            # Plot axis of camera in 3D
            ax.plot([coords[i, 0], coords[i, 0]-x_rot[0]*0.1], [coords[i, 1], coords[i, 1]-x_rot[1]*0.1], [coords[i, 2]
                    , coords[i, 2]-x_rot[2]*0.1], c='r')
            ax.plot([coords[i, 0], coords[i, 0]-y_rot[0]*0.1], [coords[i, 1], coords[i, 1]-y_rot[1]*0.1], [coords[i, 2]
                    , coords[i, 2]-y_rot[2]*0.1], c='g')
            ax.plot([coords[i, 0], coords[i, 0]-z_rot[0]*0.1], [coords[i, 1], coords[i, 1]-z_rot[1]*0.1], [coords[i, 2]
                    , coords[i, 2]-z_rot[2]*0.1], c='b')

        # Plot 3D coordinates of camera
        ax.scatter3D(x, y, z, c=timestamp, cmap='cool')
        ax.set_xlabel('X axis')
        ax.set_xlim(0, 0.5)
        ax.set_ylabel('Y axis')
        ax.set_ylim(0, -0.5)
        ax.set_zlabel('Z axis')
        ax.set_zlim(-0.4, -0.9)
        plt.show()


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
    R_tot = np.multiply(np.multiply(R_x, R_y), R_z)
    return np.dot(R_tot, matrix[0, :]), np.dot(R_tot, matrix[1, :]), np.dot(R_tot, matrix[2, :])


if __name__ == '__main__':
    main()
