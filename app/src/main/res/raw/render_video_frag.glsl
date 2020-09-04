#version 300 es
#extension GL_OES_EGL_image_external : require

precision mediump float;

in vec2 vTextureCoord;

uniform samplerExternalOES sTexture;
out vec3 frag_color;

void main(){
    frag_color = texture2D(sTexture, vTextureCoord);
}