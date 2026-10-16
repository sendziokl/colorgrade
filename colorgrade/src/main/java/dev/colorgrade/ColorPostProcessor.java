package dev.colorgrade;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Własny pełnoekranowy pass w czystym OpenGL (niezależny od wewnętrznego API post-effect Mojanga,
 * które zmienia się między wersjami). Kopiuje główny framebuffer do tekstury i rysuje go z korekcją kolorów.
 */
public final class ColorPostProcessor {
    private static final String VERT = """
            #version 330 core
            out vec2 uv;
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                uv = p;
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAG = """
            #version 330 core
            in vec2 uv;
            out vec4 fragColor;
            uniform sampler2D uScene;
            uniform vec2 uScale;
            uniform float uBrightness;
            uniform float uContrast;
            uniform float uGamma;
            uniform float uSaturation;
            uniform float uHue;
            uniform vec3 uGain;

            vec3 hueShift(vec3 c, float a) {
                float cs = cos(a), sn = sin(a);
                vec3 k = vec3(0.57735026);
                return c * cs + cross(k, c) * sn + k * dot(k, c) * (1.0 - cs);
            }

            void main() {
                vec3 c = texture(uScene, uv * uScale).rgb;
                c *= uGain;
                c = hueShift(c, uHue);
                float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
                c = mix(vec3(l), c, uSaturation);
                c = (c - 0.5) * uContrast + 0.5;
                c += uBrightness;
                c = clamp(c, 0.0, 1.0);
                c = pow(c, vec3(1.0 / uGamma));
                fragColor = vec4(c, 1.0);
            }
            """;

    private static boolean initialized = false;
    private static boolean failed = false;
    private static int program, vao, tex;
    private static int texW = 0, texH = 0;
    private static int uScene, uScale, uBrightness, uContrast, uGamma, uSaturation, uHue, uGain;

    private ColorPostProcessor() {}

    private static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile error: " + GL20.glGetShaderInfoLog(s));
        }
        return s;
    }

    private static void init() {
        initialized = true;
        try {
            int vs = compile(GL20.GL_VERTEX_SHADER, VERT);
            int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAG);
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                throw new RuntimeException("Shader link error: " + GL20.glGetProgramInfoLog(program));
            }
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);

            uScene = GL20.glGetUniformLocation(program, "uScene");
            uScale = GL20.glGetUniformLocation(program, "uScale");
            uBrightness = GL20.glGetUniformLocation(program, "uBrightness");
            uContrast = GL20.glGetUniformLocation(program, "uContrast");
            uGamma = GL20.glGetUniformLocation(program, "uGamma");
            uSaturation = GL20.glGetUniformLocation(program, "uSaturation");
            uHue = GL20.glGetUniformLocation(program, "uHue");
            uGain = GL20.glGetUniformLocation(program, "uGain");

            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(prevVao);

            tex = GL11.glGenTextures();
        } catch (Throwable t) {
            failed = true;
            ColorGradeClient.LOGGER.error("ColorGrade: nie udało się zainicjalizować shadera, efekt wyłączony", t);
        }
    }

    public static void apply(MinecraftClient mc) {
        ColorConfig cfg = ColorConfig.INSTANCE;
        if (failed || !cfg.enabled || cfg.isNeutral() || mc.world == null) return;
        if (!initialized) init();
        if (failed) return;

        Framebuffer fb = mc.getFramebuffer();
        int vw = fb.viewportWidth, vh = fb.viewportHeight;
        int tw = fb.textureWidth, th = fb.textureHeight;
        if (vw <= 0 || vh <= 0) return;

        // --- zapamiętaj stan GL (żeby cache vanilli pozostał spójny) ---
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb.fbo);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            if (tw != texW || th != texH) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, tw, th, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12_CLAMP);
                texW = tw;
                texH = th;
            }
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, vw, vh);

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(false);

            GL20.glUseProgram(program);
            GL20.glUniform1i(uScene, 0);
            GL20.glUniform2f(uScale, (float) vw / tw, (float) vh / th);
            GL20.glUniform1f(uBrightness, cfg.brightness);
            GL20.glUniform1f(uContrast, cfg.contrast);
            GL20.glUniform1f(uGamma, Math.max(0.05f, cfg.gamma));
            GL20.glUniform1f(uSaturation, cfg.saturation);
            GL20.glUniform1f(uHue, (float) Math.toRadians(cfg.hue));
            GL20.glUniform3f(uGain, cfg.red, cfg.green, cfg.blue);

            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            // --- przywróć stan ---
            GL30.glBindVertexArray(prevVao);
            GL20.glUseProgram(prevProgram);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL13.glActiveTexture(prevActive);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            GL11.glDepthMask(depthMask);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            setEnabled(GL11.GL_CULL_FACE, cull);
            setEnabled(GL11.GL_SCISSOR_TEST, scissor);
        }
    }

    private static final int GL12_CLAMP = 0x812F; // GL_CLAMP_TO_EDGE

    private static void setEnabled(int cap, boolean on) {
        if (on) GL11.glEnable(cap); else GL11.glDisable(cap);
    }
}
