//return0 wird zurück gegeben wenn
//sich die Kreise nicht schneiden
double[] return0 = {};
double AB0 = B[0] - A[0];
double AB1 = B[1] - A[1];
double c = Math.sqrt(AB0 * AB0 + AB1 * AB1);

if(c == 0) {
	return return0;
}

double x = (a*a + c*c - b*b) / (2*c);
double y = a*a - x*x;

if(y < 0) {
	return return0;
} else if (y > 0) {
	y = Math.sqrt(y);
}

double ex0 = AB0 / c;
double ex1 = AB1 / c;
double ey0 = -ex1;
double ey1 = ex0;
double Q1x = A[0] + x * ex0;
double Q1y = A[1] + x * ex1;

if(y == 0) {
	//return1 wird zurück gegeben wenn
	//sich die Kreise an endlos vielen Punkten schneiden
	//bzw. die Distanz zwischen den Kreisezentren 0 beträgt
	double[] return1 = {Q1x, Q1y};
	return return1;
}

double Q2x = Q1x - y * ey0;
double Q2y = Q1y - y * ey1;
Q1x += y * ey0;
Q1y += y * ey1;

//return2 wird zurück gegeben wenn
//sich die Kreise an zwei Punkten schneiden
double[] return2 = {Q1x, Q1y, Q2x, Q2y};
return return2;