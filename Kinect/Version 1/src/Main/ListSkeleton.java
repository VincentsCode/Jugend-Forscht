package Main;

import KinectPV2.Skeleton;

public class ListSkeleton {
	
	Skeleton skeleton;
	int id;

	ListSkeleton(Skeleton s, int i) {
		this.id = i;
		this.skeleton = s;
	}
}
