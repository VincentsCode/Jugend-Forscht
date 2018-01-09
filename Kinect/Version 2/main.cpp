#include <iostream>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <Windows.h>
#include <Kinect.h>
#include <wrl/client.h>
#include <opencv2/core.hpp>
#include <opencv2/tracking.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/calib3d.hpp>
#include <opencv2/xfeatures2d.hpp>

#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")

using namespace Microsoft::WRL;

// constants
const int depthWidth = 512;
const int depthHeight = 424;
const int depthBytesPerPixel = 2;
const int colorWidth = 1920;
const int colorHeight = 1080;
const int colorBytesPerPixel = 4;

const char ip[14] = "192.168.43.8";
const char port[5] = "1337";

// variables
ComPtr<IKinectSensor> kinect;

ComPtr<ICoordinateMapper> coordinateMapper;

DepthSpacePoint* depthCoordinates = new DepthSpacePoint[colorWidth * colorHeight];

ComPtr<IColorFrameSource> colorFrameSource;
ComPtr<IColorFrameReader> colorFrameReader;
ComPtr<IColorFrame> colorFrame;
UINT colorBufferSize;
RGBQUAD* colorBuffer;
RGBQUAD* colorRGBX = new RGBQUAD[colorWidth * colorHeight];

ComPtr<IDepthFrameSource> depthFrameSource;
ComPtr<IDepthFrameReader> depthFrameReader;
ComPtr<IDepthFrame> depthFrame;
UINT depthBufferSize;
UINT16* depthBuffer;

ComPtr<IBodyIndexFrameSource> bodyIndexFrameSource;
ComPtr<IBodyIndexFrameReader> bodyIndexFrameReader;
ComPtr<IBodyIndexFrame> bodyIndexFrame;
UINT bodyIndexBufferSize;
BYTE* bodyIndexBuffer;

ComPtr<IBodyFrameSource> bodyFrameSource;
ComPtr<IBodyFrameReader> bodyFrameReader;
ComPtr<IBodyFrame> bodyFrame;
IBody* bodies[BODY_COUNT];

RGBQUAD* outputRGBX = new RGBQUAD[colorWidth * colorHeight];
RGBQUAD* backgroundRGBX = new RGBQUAD[colorWidth * colorHeight];

cv::Ptr<cv::Tracker> tracker;
cv::Rect2d bbox;

SOCKET wlanSocket;

// functions
void init();
bool updateData();
int personInFrontOfSensor(float range = 0.2);
IBody* getPersonInFrontOfSensor(float range = 0.2);
cv::Mat getColorMat();
cv::Mat getGreenscreenImage();
cv::Mat getColorInDepthRange(int minDepth, int maxDepth);
bool isInRange(cv::Point p, int minDepth, int maxDepth);

int main() {

	// init
	init();

	// setup
	Sleep(2000);
	std::cout << "Setup started. Waiting for Person." << std::endl;
	do {
		updateData();
	} while (!personInFrontOfSensor());

	IBody* body = nullptr;
	body = getPersonInFrontOfSensor();
	Joint joints[JointType_Count];
	body->GetJoints(_countof(joints), joints);
	int minX = 1920;
	int maxX = 0;
	int minZ = 8000;
	int maxZ = 0;
	for (int i = 0; i < JointType_Count; i++) {
		Joint j = joints[i];
		if (j.TrackingState == TrackingState_Tracked) {
			int zPosition = j.Position.Z * 1000;
			if (zPosition > maxZ) {
				maxZ = zPosition;
			}
			if (zPosition < minZ) {
				minZ = zPosition;
			}

			CameraSpacePoint cameraSpacePoint = j.Position;
			ColorSpacePoint colorSpacePoint;
			coordinateMapper->MapCameraPointToColorSpace(cameraSpacePoint, &colorSpacePoint);
			int xPosition = colorSpacePoint.X;
			if (xPosition > maxX) {
				maxX = xPosition;
			}
			if (xPosition < minX) {
				minX = xPosition;
			}
		}
	}

	int personWidth = maxX - minX;
	int personDepth = maxZ - minZ;

	int r = 15;
	int bboxX, bboxY, bboxW, bboxH;
	bboxX = minX - r;
	bboxW = personWidth + r;

	CameraSpacePoint cameraSpacePoint = joints[8].Position;
	ColorSpacePoint colorSpacePoint;
	coordinateMapper->MapCameraPointToColorSpace(cameraSpacePoint, &colorSpacePoint);
	bboxY = colorSpacePoint.Y - r;
	cameraSpacePoint = joints[12].Position;
	colorSpacePoint;
	coordinateMapper->MapCameraPointToColorSpace(cameraSpacePoint, &colorSpacePoint);
	bboxH = std::abs(bboxY - colorSpacePoint.Y) + 15;

	bbox = cv::Rect2d(bboxX, bboxY, bboxW, bboxH);

	cv::Mat m = getColorInDepthRange(minZ - 50, maxZ + 50);

	tracker->init(m, bbox);

	// loop
	int differ = personDepth / 10;
	int variance = personDepth / 2;
	while (true) {
		bool updated = updateData();
		if (updated) {
			double timer = (double) cv::getTickCount();
			cv::Mat m = getColorInDepthRange(minZ - differ, maxZ + differ);
			cvtColor(m, m, CV_BGRA2BGR);

			bool TRACKING_OK = tracker->update(m, bbox);
			if (TRACKING_OK) {
				rectangle(m, bbox, cv::Scalar(255, 0, 255), 2, 1);
				cv::Point p = cv::Point(bbox.x + bbox.width / 2, bbox.y + bbox.height / 2);
				cv::circle(m, p, 2, cv::Scalar(255, 0, 255), 4);

				int nMinZ = 8000;
				int leftCount = 0;
				int midCount = 0;
				int rightCount = 0;

				coordinateMapper->MapColorFrameToDepthSpace(depthWidth * depthHeight, (UINT16*)depthBuffer, colorWidth * colorHeight, depthCoordinates);
				for (int i = 0; i < (bbox.width / 3); i += 20) {
					for (int j = 0; j < bbox.height; j+=20) {
						cv::Point x = cv::Point(bbox.x + i, bbox.y + j);
						int pIndex = x.y*colorWidth + x.x;
						int z;
						if (pIndex > 0 && pIndex < (colorWidth * colorHeight)) {
							DepthSpacePoint p = depthCoordinates[pIndex];
							if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
								int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);
								if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
									z = depthBuffer[depthX + (depthY * depthWidth)];
								}
							}
							if (z > minZ && z < maxZ) {
								cv::circle(m, x, 2, cv::Scalar(0, 0, 255), 4);
								leftCount++;
							}
							if (z > minZ - variance && z < maxZ + variance) {
								if (z < nMinZ) {
									nMinZ = z;
								}
							}
						}
					}
				}
				for (int i = 0; i < (bbox.width / 3); i += 20) {
					for (int j = 0; j < bbox.height; j += 20) {
						cv::Point x = cv::Point(bbox.x + i + (bbox.width / 3), bbox.y + j);
						int pIndex = x.y*colorWidth + x.x;
						int z;
						if (pIndex > 0 && pIndex < (colorWidth * colorHeight)) {
							DepthSpacePoint p = depthCoordinates[pIndex];
							if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
								int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);
								if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
									z = depthBuffer[depthX + (depthY * depthWidth)];
								}
							}
							if (z > minZ && z < maxZ) {
								cv::circle(m, x, 2, cv::Scalar(0, 255, 0), 4);
								midCount++;
							}
							if (z > minZ - variance && z < maxZ + variance) {
								if (z < nMinZ) {
									nMinZ = z;
								}
							}
						}
					}
				}
				for (int i = 0; i < (bbox.width / 3); i += 20) {
					for (int j = 0; j < bbox.height; j += 20) {
						cv::Point x = cv::Point(bbox.x + i + ((bbox.width / 3) * 2), bbox.y + j);
						int pIndex = x.y*colorWidth + x.x;
						int z;
						if (pIndex > 0 && pIndex < (colorWidth * colorHeight)) {
							DepthSpacePoint p = depthCoordinates[pIndex];
							if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
								int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);
								if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
									z = depthBuffer[depthX + (depthY * depthWidth)];
								}
							}
							if (z > minZ && z < maxZ) {
								cv::circle(m, x, 2, cv::Scalar(255, 0, 0), 4);
								rightCount++;
							}
							if (z > minZ - variance && z < maxZ + variance) {
								if (z < nMinZ) {
									nMinZ = z;
								}
							}
						}
					}
				}

				minZ = nMinZ - differ;
				maxZ = nMinZ + personDepth;

				std::string str = std::to_string(p.x - (colorWidth / 2)) + "#" + std::to_string(minZ);
				send(wlanSocket, str.c_str(), (int)strlen(str.c_str()), 0);
			}
			else {
				putText(m, "Tracking failure detected", cv::Point(100, 80), cv::FONT_HERSHEY_SIMPLEX, 0.75, cv::Scalar(255, 0, 255), 2);
			}

			float fps = cv::getTickFrequency() / ((double) cv::getTickCount() - timer);
			putText(m, "FPS: " + std::to_string(fps), cv::Point(100, 50), cv::FONT_HERSHEY_SIMPLEX, 0.75, cv::Scalar(50, 170, 50), 2);

			imshow("M", m);

			int k = cv::waitKey(1);
			if (k == 27) {
				break;
			}
		}
	}
	kinect->Close();
	shutdown(wlanSocket, SD_SEND);
	closesocket(wlanSocket);
	WSACleanup();
	cvDestroyAllWindows();
	return 0;
}

void init() {
	// init kinect
	GetDefaultKinectSensor(&kinect);
	kinect->Open();

	// init coordinateMapper
	kinect->get_CoordinateMapper(&coordinateMapper);

	// init colorStream
	kinect->get_ColorFrameSource(&colorFrameSource);
	colorFrameSource->OpenReader(&colorFrameReader);

	// init depthStream
	kinect->get_DepthFrameSource(&depthFrameSource);
	depthFrameSource->OpenReader(&depthFrameReader);

	// init bodyIndexStream
	kinect->get_BodyIndexFrameSource(&bodyIndexFrameSource);
	bodyIndexFrameSource->OpenReader(&bodyIndexFrameReader);

	// init bodyStreamy
	kinect->get_BodyFrameSource(&bodyFrameSource);
	bodyFrameSource->OpenReader(&bodyFrameReader);

	// init OpenCV / tracker
	cv::setUseOptimized(true);
	tracker = cv::TrackerMOSSE::create();

	// init client-connection to the ev3
	WSADATA wsaData;
	struct addrinfo *result = NULL, *ptr = NULL, hints;
	WSAStartup(MAKEWORD(2, 2), &wsaData);
	ZeroMemory(&hints, sizeof(hints));
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	getaddrinfo(ip, port, &hints, &result);
	for (ptr = result; ptr != NULL; ptr = ptr->ai_next) {
		wlanSocket = socket(ptr->ai_family, ptr->ai_socktype, ptr->ai_protocol);
		connect(wlanSocket, ptr->ai_addr, (int)ptr->ai_addrlen);
		break;
	}
	freeaddrinfo(result);

	// time the sensor needs to startup
	Sleep(2000);
}

bool updateData() {
	HRESULT hr;

	// get colorFrame and colorBuffer
	hr = colorFrameReader->AcquireLatestFrame(&colorFrame);
	if (FAILED(hr)) {
		return false;
	} else {
		colorBuffer = colorRGBX;
		colorBufferSize = colorWidth * colorHeight * sizeof(RGBQUAD);
		hr = colorFrame->CopyConvertedFrameDataToArray(colorBufferSize, reinterpret_cast<BYTE*>(colorBuffer), ColorImageFormat_Bgra);
		if (FAILED(hr)) {
			return false;
		}
	}

	// get depthFrame and depthBuffer
	hr = depthFrameReader->AcquireLatestFrame(&depthFrame);
	if (FAILED(hr)) {
		return false;
	} else {
		hr = depthFrame->AccessUnderlyingBuffer(&depthBufferSize, &depthBuffer);
		if (FAILED(hr)) {
			return false;
		}
	}

	// get bodyIndexFrame and bodyIndexBuffer
	hr = bodyIndexFrameReader->AcquireLatestFrame(&bodyIndexFrame);
	if (FAILED(hr)) {
		return false;
	} else {
		hr = bodyIndexFrame->AccessUnderlyingBuffer(&bodyIndexBufferSize, &bodyIndexBuffer);
		if (FAILED(hr)) {
			return false;
		}
	}

	// get bodyFrame and bodies
	hr = bodyFrameReader->AcquireLatestFrame(&bodyFrame);
	if (FAILED(hr)) {
		return false;
	} else {
		hr = bodyFrame->GetAndRefreshBodyData(_countof(bodies), bodies);
		if (FAILED(hr)) {
			return false;
		}
	}

	// return true if nothing failed
	return true;
}

int personInFrontOfSensor(float range) {
	HRESULT hr;
	int trackCount = 0;
	int goodTrackCount = 0;

	for (int i = 0; i < BODY_COUNT; ++i) {
		if (trackCount < 1) {
			IBody* body = bodies[i];
			if (body) {
				BOOLEAN bTracked = false;
				hr = body->get_IsTracked(&bTracked);

				if (SUCCEEDED(hr) && bTracked) {
					trackCount++;
					Joint joints[JointType_Count];
					hr = body->GetJoints(_countof(joints), joints);
					Joint spineJoint = joints[1];
					if (SUCCEEDED(hr)) {
						if (spineJoint.Position.X < range && spineJoint.Position.X > -range) {
							goodTrackCount++;
						}
					}
				}
			}
		}
	}

	if (trackCount > 1) {
		return 0;
	}
	else if (trackCount == 0) {
		return 0;
	}
	if (goodTrackCount == 1 && trackCount == 1) {
		return 1;
	}
	else if (trackCount == 1) {
		return 0;
	}
}

IBody* getPersonInFrontOfSensor(float range) {
	HRESULT hr;
	int trackCount = 0;
	int goodTrackCount = 0;
	IBody* returnBody;

	for (int i = 0; i < BODY_COUNT; ++i) {
		if (trackCount < 1) {
			IBody* body = bodies[i];
			if (body) {
				BOOLEAN bTracked = false;
				hr = body->get_IsTracked(&bTracked);

				if (SUCCEEDED(hr) && bTracked) {
					trackCount++;
					Joint joints[JointType_Count];
					hr = body->GetJoints(_countof(joints), joints);
					Joint spineJoint = joints[1];
					if (SUCCEEDED(hr)) {
						return body;
					}
				}
			}
		}
	}	
}

cv::Mat getColorMat() {
	std::vector<BYTE> matBuffer(colorWidth * colorHeight * colorBytesPerPixel);
	colorFrame->CopyConvertedFrameDataToArray(static_cast<UINT>(matBuffer.size()), &matBuffer[0], ColorImageFormat::ColorImageFormat_Bgra);
	cv::Mat m = cv::Mat(colorHeight, colorWidth, CV_8UC4, &matBuffer[0]);
	cvtColor(m, m, CV_BGRA2BGR);
	return m;
}

cv::Mat getGreenscreenImage() {
	HRESULT hr = coordinateMapper->MapColorFrameToDepthSpace(depthWidth * depthHeight, (UINT16*)depthBuffer, colorWidth * colorHeight, depthCoordinates);
	if (SUCCEEDED(hr)) {
		for (int colorIndex = 0; colorIndex < (colorWidth*colorHeight); ++colorIndex) {

			const RGBQUAD* pSrc = backgroundRGBX + colorIndex;
			DepthSpacePoint p = depthCoordinates[colorIndex];

			if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
				int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);

				if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
					BYTE player = bodyIndexBuffer[depthX + (depthY * depthWidth)];
					if (player != 0xff) {
						pSrc = colorRGBX + colorIndex;
					}
				}
			}
			outputRGBX[colorIndex] = *pSrc;
		}
	}
	return cv::Mat(1080, 1920, CV_8UC4, outputRGBX);
}

cv::Mat getColorInDepthRange(int minDepth, int maxDepth) {
	HRESULT hr = coordinateMapper->MapColorFrameToDepthSpace(depthWidth * depthHeight, (UINT16*)depthBuffer, colorWidth * colorHeight, depthCoordinates);
	if (SUCCEEDED(hr)) {
		for (int colorIndex = 0; colorIndex < (colorWidth*colorHeight); ++colorIndex) {

			RGBQUAD* pSrc = backgroundRGBX + colorIndex;
			DepthSpacePoint p = depthCoordinates[colorIndex];

			if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
				int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);

				if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
					int depthZ = depthBuffer[depthX + (depthY * depthWidth)];
					if (depthZ > minDepth && depthZ < maxDepth) {
						pSrc = colorRGBX + colorIndex;
					}
				}
			}
			outputRGBX[colorIndex] = *pSrc;
		}
	}
	return cv::Mat(1080, 1920, CV_8UC4, outputRGBX);
}

bool isInRange(cv::Point p, int minDepth, int maxDepth) {
	HRESULT hr = coordinateMapper->MapColorFrameToDepthSpace(depthWidth * depthHeight, (UINT16*)depthBuffer, colorWidth * colorHeight, depthCoordinates);
	if (SUCCEEDED(hr)) {
		int index = p.y*colorWidth + p.x;
		if (index > 0) {
			DepthSpacePoint p = depthCoordinates[index];
			if (p.X != -std::numeric_limits<float>::infinity() && p.Y != -std::numeric_limits<float>::infinity()) {
				int depthX = static_cast<int>(p.X + 0.5f); int depthY = static_cast<int>(p.Y + 0.5f);

				if ((depthX >= 0 && depthX < depthWidth) && (depthY >= 0 && depthY < depthHeight)) {
					int depthZ = depthBuffer[depthX + (depthY * depthWidth)];
					if (depthZ > minDepth && depthZ < maxDepth) {
						return true;
					}
				}
			}
		}

	}
	return false;
}