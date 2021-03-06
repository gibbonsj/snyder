/*
Snyder Projection Implementation

Copyright (c) 2012-2015, APIS Point, LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package gis.proj.projections;

import static gis.proj.SnyderMath.*;

import gis.proj.Datum;
import gis.proj.Ellipsoid;
import gis.proj.Miscellaneous;
import gis.proj.Spherical;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * All formulas adopted from USGS Professional Paper 1395.
 *
 * References:
 *
 * Map Projections - A Working Manual, USGS Professional Paper 1395
 * John P. Snyder
 * Pages 239-242
 *
 */

public final class VanDerGrinten implements Miscellaneous, Spherical {

    public String getName() {
        return "Van der Grinten";
    }

    public double[][] inverse(double[] x, double[] y, Ellipsoid ellip, Datum datum) {
        double R    = ellip.getProperty("R");

        double lon0 = datum.getProperty("lon0");

        double[] lon = new double[x.length];
        double[] lat = new double[y.length];

        double piR = PI * R;

        double X, Y, c1, c2, c3, d, a1, m1, theta1, XX, YY;

        for(int i = 0; i < lon.length; ++i) {
            X  = x[i] / piR;
            XX = X*X;
            Y  = y[i] / piR;
            YY = Y*Y;

            c1 = -StrictMath.abs(Y) * (1.0 + XX + YY);
            c2 = c1 - 2.0 * YY + XX;
            c3 = -2.0 * c1 + 1.0 + 2.0 * YY + (XX + YY) * (XX + YY);
            d  = YY / c3 + (2.0 * (c2 * c2 * c2) / (c3 * c3 * c3) - 9.0 * c1 * c2 / (c3 * c3)) / 27.0;
            a1 = (c1 - (c2 * c2) / (3.0 * c3)) / c3;
            m1 = 2.0 * StrictMath.sqrt(-a1 / 3.0);

            theta1 = (1.0/3.0) * StrictMath.acos((3.0 * d) / (a1 * m1));

            if(StrictMath.abs(X) < NEAR_ZERO_DEG)
                lon[i] = lon0;
            else
                lon[i] = normalizeLonRad(
                        PI * (XX +YY-1.0+StrictMath.sqrt(1.0+2.0*(XX-YY) +(XX+YY)*(XX+YY))) / (2.0 * X) + lon0
                        );

            lat[i] = StrictMath.copySign(PI * (-m1 * StrictMath.cos(theta1 + PI / 3.0) - c2 / (3.0 * c3)), y[i]);
        }

        return new double[][] {lon, lat};
    }

    public double[][] forward(double[] lon, double[] lat, Ellipsoid ellip, Datum datum) {
        double R    = ellip.getProperty("R");

        double lon0 = datum.getProperty("lon0");

        double[] x = new double[lon.length];
        double[] y = new double[lat.length];

        double piR         = PI * R;
        double two_over_pi = 2.0 / PI;

        double lon_M_lon0, theta, A, G, P, Q, AA, PP, AA_P_PP, G_M_PP;

        for(int i = 0; i < lon.length; ++i) {
            lon_M_lon0 = normalizeLonRad(lon[i] - lon0);
            theta = StrictMath.asin(StrictMath.abs(lat[i] * two_over_pi));

            A = 0.5 * StrictMath.abs(PI / lon_M_lon0 - lon_M_lon0 / PI);
            AA = A * A;
            G = StrictMath.cos(theta) / (StrictMath.sin(theta) + StrictMath.cos(theta) - 1.0);
            P = G * (2.0 / StrictMath.sin(theta) - 1.0);
            PP = P * P;
            Q = AA + G;
            G_M_PP = G - PP;
            AA_P_PP = AA + PP;

            if(StrictMath.abs(lat[i]) < NEAR_ZERO_RAD) {
                x[i] = R * lon_M_lon0;
                y[i] = 0.0;
            }
            else if(StrictMath.abs(lon_M_lon0) < NEAR_ZERO_RAD) {
                x[i] = 0.0;
                y[i] = StrictMath.copySign(piR * StrictMath.tan(theta * 0.5), lat[i]);
            }
            else {
                x[i] = StrictMath.copySign(piR * (
                        A * G_M_PP + StrictMath.sqrt(AA * G_M_PP * G_M_PP - AA_P_PP * (G * G - PP))
                        ) / AA_P_PP, lon_M_lon0);

                y[i] =  StrictMath.copySign(piR * (
                        P * Q - A * StrictMath.sqrt((AA + 1.0) * AA_P_PP - Q * Q)
                        ) / AA_P_PP, lat[i]);
            }
        }

        return new double[][] {x, y};
    }

    public Set<String> getDatumProperties() {
        return new HashSet<String>(Arrays.asList(new String[]{
                "lon0"}));
    }

}