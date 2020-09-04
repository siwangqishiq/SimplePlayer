#version 300 es

uniform mat4 uMVPMatrix; //总变换矩阵
uniform mat4 uSTMatrix;

in vec4 aPosition;  //顶点位置
in vec4 aTextureCoord;    //纹理坐标
out vec2 vTextureCoord;

void main()  {
    gl_Position = uMVPMatrix * aPosition;
    vPosition=vTextureCoord = (uSTMatrix * aTextureCoord).xy;
}