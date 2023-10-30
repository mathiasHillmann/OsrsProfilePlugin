/*
 * Copyright (c) 2020 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Sourced from https://github.com/Bram91/Model-Dumper
 */
package com.osrsprofile.exporter;

import net.runelite.api.Client;
import net.runelite.api.Model;

import javax.inject.Inject;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerModelExporter {
    @Inject
    private Client client;

    @Inject
    private TextureColor textureColor;

    public ByteArrayOutputStream export() throws IOException
    {
        List<Vertex> vertices = new ArrayList<>();
        Model model = client.getLocalPlayer().getModel();

        for (int fi=0; fi < model.getFaceCount(); fi++)
        {
            // determine vertex colors (textured or colored?)
            Color vc1;
            Color vc2;
            Color vc3;
            int textureId = -1;

            if (model.getFaceTextures() != null) {
                textureId = model.getFaceTextures()[fi];
            }

            if (textureId != -1) {
                // get average color of texture
                vc1 = this.textureColor.getColor(textureId);
                vc2 = vc1;
                vc3 = vc1;
            } else {
                // get average color of vertices
                vc1 = new Color(JagexColor.HSLtoRGB((short) model.getFaceColors1()[fi], JagexColor.BRIGHTNESS_MIN));
                vc2 = new Color(JagexColor.HSLtoRGB((short) model.getFaceColors2()[fi], JagexColor.BRIGHTNESS_MIN));
                vc3 = new Color(JagexColor.HSLtoRGB((short) model.getFaceColors3()[fi], JagexColor.BRIGHTNESS_MIN));
            }

            int vi1 = model.getFaceIndices1()[fi];
            int vi2 = model.getFaceIndices2()[fi];
            int vi3 = model.getFaceIndices3()[fi];

            int vx1 = model.getVerticesX()[vi1];
            int vx2 = model.getVerticesX()[vi2];
            int vx3 = model.getVerticesX()[vi3];
            int vy1 = -model.getVerticesY()[vi1];
            int vy2 = -model.getVerticesY()[vi2];
            int vy3 = -model.getVerticesY()[vi3];
            int vz1 = model.getVerticesZ()[vi1];
            int vz2 = model.getVerticesZ()[vi2];
            int vz3 = model.getVerticesZ()[vi3];

            vertices.add(new Vertex(vx1, vy1, vz1, vc1.getRed(), vc1.getGreen(), vc1.getBlue()));
            vertices.add(new Vertex(vx2, vy2, vz2, vc2.getRed(), vc2.getGreen(), vc2.getBlue()));
            vertices.add(new Vertex(vx3, vy3, vz3, vc3.getRed(), vc3.getGreen(), vc3.getBlue()));
        }

        ByteArrayOutputStream ply = new ByteArrayOutputStream();

        PrintWriter plyHeader = new PrintWriter(ply);
        plyHeader.println("ply");
        plyHeader.println("format binary_little_endian 1.0");
        plyHeader.println("element vertex " + vertices.size());
        plyHeader.println("property int16 x");
        plyHeader.println("property int16 y");
        plyHeader.println("property int16 z");
        plyHeader.println("property uint8 red");
        plyHeader.println("property uint8 green");
        plyHeader.println("property uint8 blue");
        plyHeader.println("element face " + model.getFaceCount());
        plyHeader.println("property list uint8 int16 vertex_indices");
        plyHeader.println("end_header");
        plyHeader.flush();

        for (Vertex v: vertices) {
            // Y and Z axes are flipped
            ply.write(le(v.x));
            ply.write(le(v.z));
            ply.write(le(v.y));
            ply.write((byte) v.r);
            ply.write((byte) v.g);
            ply.write((byte) v.b);
        }

        for (int i=0; i < model.getFaceCount(); ++i) {
            int vi = i*3;
            ply.write((byte) 3);
            ply.write(le(vi));
            ply.write(le(vi+1));
            ply.write(le(vi+2));
        }

        ply.flush();

        return ply;
    }

    // int to little endian byte array
    private static byte[] le(int n)
    {
        byte[] b = new byte[2];
        b[0] = (byte) n;
        b[1] = (byte) (n >> 8);
        return b;
    }

    private static class Vertex
    {
        public int x, y, z;
        public int r, g, b;

        public Vertex(int x, int y, int z, int r, int g, int b)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return x == vertex.x && y == vertex.y && z == vertex.z && r == vertex.r && g == vertex.g && b == vertex.b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, r, g, b);
        }
    }
}
