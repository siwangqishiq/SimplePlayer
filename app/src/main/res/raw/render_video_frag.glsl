#version 300 es
//#extension GL_OES_EGL_image_external_essl3 : require
#extension GL_OES_EGL_image_external_essl3 : enable

precision mediump float;

in vec2 vTextureCoord;

uniform samplerExternalOES sTexture;
out vec3 frag_color;

void main(){
    frag_color = texture(sTexture, vTextureCoord).rgb;
}