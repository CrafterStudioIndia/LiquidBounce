<script lang="ts">
    import { onMount, onDestroy, createEventDispatcher } from "svelte";
    import { equipClientCape, openCapeArtStudio } from "../../../integration/rest";
    import { notification } from "../common/header/notification_store";

    export let playerName: string = "Player";

    const dispatch = createEventDispatcher();

    let capeWidth = 64;
    let capeHeight = 32;
    let isHd = false;

    let activeTool: "pencil" | "eraser" | "eyedropper" | "bucket" = "pencil";
    let activeColor = "#F5BE23";

    const PALETTE = [
        "#F5BE23", "#EAB308", "#B4820F", "#00A8A8",
        "#38BDF8", "#8B5CF6", "#EC4899", "#EF4444",
        "#10B981", "#1E293B", "#000000", "#FFFFFF",
        "#C68642", "#5C4033", "#A0522D", "#696969"
    ];

    let pixelCanvasEl: HTMLCanvasElement;
    let previewCanvasEl: HTMLCanvasElement;

    let isDrawing = false;
    let undoStack: ImageData[] = [];
    let loading = false;

    // WebGL 3D Preview State
    let gl: WebGLRenderingContext | null = null;
    let shaderProg: WebGLProgram | null = null;
    let capeTex: WebGLTexture | null = null;
    let skinTex: WebGLTexture | null = null;
    let animFrame: number | null = null;
    let flapTime = 0;
    let isFlapping = true;
    let camRotX = 14;
    let camRotY = 160; // 160° shows player back with cape
    let camDist = 42;
    let isDragging3D = false;
    let lastX = 0, lastY = 0;
    let previewView: "back" | "front" = "back";

    let headBox: any, hatBox: any, torsoBox: any, armRBox: any, armLBox: any, legRBox: any, legLBox: any, capeBox: any;
    let aPos = 0, aUV = 0, uMat: WebGLUniformLocation | null = null, uSamp: WebGLUniformLocation | null = null, uColor: WebGLUniformLocation | null = null;

    onMount(() => {
        initPixelCanvas();
        init3DPreview();
    });

    onDestroy(() => {
        if (animFrame) {
            cancelAnimationFrame(animFrame);
            animFrame = null;
        }
    });

    function initPixelCanvas() {
        if (!pixelCanvasEl) return;
        const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
        if (!ctx) return;

        // Draw initial default gold cape body
        ctx.fillStyle = "#F5BE23";
        ctx.fillRect(0, 0, capeWidth, capeHeight);
        ctx.fillStyle = "#B4820F";
        ctx.strokeRect(1, 1, 20, 15);
        ctx.fillStyle = "#FFFFFF";
        ctx.fillRect(9, 5, 4, 7);
        ctx.fillStyle = "#EAB308";
        ctx.fillRect(22, 1, 20, 15);

        saveUndo();
        update3DCapeTexture();
    }

    function setResolution(hd: boolean) {
        if (isHd === hd) return;
        isHd = hd;
        capeWidth = hd ? 128 : 64;
        capeHeight = hd ? 64 : 32;

        if (pixelCanvasEl) {
            const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
            if (ctx) {
                const oldData = ctx.getImageData(0, 0, pixelCanvasEl.width, pixelCanvasEl.height);
                pixelCanvasEl.width = capeWidth;
                pixelCanvasEl.height = capeHeight;
                ctx.fillStyle = "#F5BE23";
                ctx.fillRect(0, 0, capeWidth, capeHeight);
                ctx.imageSmoothingEnabled = false;
                ctx.drawImage(oldData as any, 0, 0, capeWidth, capeHeight);
            }
        }
        undoStack = [];
        saveUndo();
        update3DCapeTexture();
    }

    function saveUndo() {
        if (!pixelCanvasEl) return;
        const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
        if (!ctx) return;
        undoStack.push(ctx.getImageData(0, 0, capeWidth, capeHeight));
        if (undoStack.length > 25) undoStack.shift();
    }

    function undo() {
        if (undoStack.length <= 1 || !pixelCanvasEl) return;
        undoStack.pop();
        const prev = undoStack[undoStack.length - 1];
        const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
        if (ctx && prev) {
            ctx.putImageData(prev, 0, 0);
            update3DCapeTexture();
        }
    }

    function clearCanvas() {
        if (!pixelCanvasEl) return;
        const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
        if (!ctx) return;
        ctx.fillStyle = "#F5BE23";
        ctx.fillRect(0, 0, capeWidth, capeHeight);
        saveUndo();
        update3DCapeTexture();
    }

    function getCoords(e: PointerEvent): { x: number; y: number } {
        const rect = pixelCanvasEl.getBoundingClientRect();
        const scaleX = capeWidth / rect.width;
        const scaleY = capeHeight / rect.height;
        const x = Math.floor((e.clientX - rect.left) * scaleX);
        const y = Math.floor((e.clientY - rect.top) * scaleY);
        return {
            x: Math.max(0, Math.min(capeWidth - 1, x)),
            y: Math.max(0, Math.min(capeHeight - 1, y))
        };
    }

    function handlePointerDown(e: PointerEvent) {
        isDrawing = true;
        pixelCanvasEl.setPointerCapture(e.pointerId);
        applyTool(e);
    }

    function handlePointerMove(e: PointerEvent) {
        if (!isDrawing) return;
        applyTool(e);
    }

    function handlePointerUp(e: PointerEvent) {
        if (!isDrawing) return;
        isDrawing = false;
        try { pixelCanvasEl.releasePointerCapture(e.pointerId); } catch (_) {}
        saveUndo();
        update3DCapeTexture();
    }

    function applyTool(e: PointerEvent) {
        const { x, y } = getCoords(e);
        const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
        if (!ctx) return;

        if (activeTool === "eyedropper") {
            const pixel = ctx.getImageData(x, y, 1, 1).data;
            const hex = "#" + ((1 << 24) + (pixel[0] << 16) + (pixel[1] << 8) + pixel[2]).toString(16).slice(1);
            activeColor = hex;
            activeTool = "pencil";
            return;
        }

        if (activeTool === "eraser") {
            ctx.clearRect(x, y, 1, 1);
            update3DCapeTexture();
            return;
        }

        if (activeTool === "bucket") {
            floodFill(ctx, x, y, activeColor);
            update3DCapeTexture();
            return;
        }

        // Pencil
        ctx.fillStyle = activeColor;
        ctx.fillRect(x, y, 1, 1);
        update3DCapeTexture();
    }

    function floodFill(ctx: CanvasRenderingContext2D, startX: number, startY: number, fillColor: string) {
        const imgData = ctx.getImageData(0, 0, capeWidth, capeHeight);
        const data = imgData.data;

        // Parse target color
        const tc = parseHex(fillColor);
        const startIdx = (startY * capeWidth + startX) * 4;
        const sr = data[startIdx], sg = data[startIdx + 1], sb = data[startIdx + 2], sa = data[startIdx + 3];

        if (sr === tc.r && sg === tc.g && sb === tc.b && sa === tc.a) return;

        const stack: [number, number][] = [[startX, startY]];
        const visited = new Uint8Array(capeWidth * capeHeight);

        while (stack.length > 0) {
            const [cx, cy] = stack.pop()!;
            const idx = (cy * capeWidth + cx) * 4;
            const vIdx = cy * capeWidth + cx;

            if (visited[vIdx]) continue;
            visited[vIdx] = 1;

            if (data[idx] === sr && data[idx + 1] === sg && data[idx + 2] === sb && data[idx + 3] === sa) {
                data[idx] = tc.r;
                data[idx + 1] = tc.g;
                data[idx + 2] = tc.b;
                data[idx + 3] = tc.a;

                if (cx > 0) stack.push([cx - 1, cy]);
                if (cx < capeWidth - 1) stack.push([cx + 1, cy]);
                if (cy > 0) stack.push([cx, cy - 1]);
                if (cy < capeHeight - 1) stack.push([cx, cy + 1]);
            }
        }
        ctx.putImageData(imgData, 0, 0);
    }

    function parseHex(hex: string) {
        let clean = hex.replace("#", "");
        if (clean.length === 3) clean = clean.split("").map(c => c + c).join("");
        const num = parseInt(clean, 16);
        return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255, a: 255 };
    }

    // ==========================================
    // WebGL 3D Player & Flapping Cape Preview
    // ==========================================
    function init3DPreview() {
        if (!previewCanvasEl) return;
        gl = previewCanvasEl.getContext("webgl", { preserveDrawingBuffer: true, alpha: true }) ||
             previewCanvasEl.getContext("experimental-webgl", { preserveDrawingBuffer: true, alpha: true }) as any;
        if (!gl) return;

        // Pointer controls for 3D orbit
        previewCanvasEl.addEventListener("pointerdown", (e) => {
            isDragging3D = true;
            lastX = e.clientX;
            lastY = e.clientY;
            previewCanvasEl.setPointerCapture(e.pointerId);
        });
        previewCanvasEl.addEventListener("pointermove", (e) => {
            if (!isDragging3D) return;
            const dx = e.clientX - lastX;
            const dy = e.clientY - lastY;
            camRotY += dx * 0.8;
            camRotX = Math.max(-85, Math.min(85, camRotX + dy * 0.8));
            lastX = e.clientX;
            lastY = e.clientY;
        });
        previewCanvasEl.addEventListener("pointerup", (e) => {
            isDragging3D = false;
            try { previewCanvasEl.releasePointerCapture(e.pointerId); } catch (_) {}
        });
        previewCanvasEl.addEventListener("wheel", (e) => {
            e.preventDefault();
            camDist = Math.max(22, Math.min(75, camDist + e.deltaY * 0.05));
        }, { passive: false });

        // Shaders
        const vsSrc = `
            attribute vec3 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMatrix * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
            }
        `;
        const fsSrc = `
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uSampler;
            uniform vec4 uColor;
            void main() {
                vec4 tex = texture2D(uSampler, vTexCoord);
                if (tex.a < 0.05) discard;
                gl_FragColor = tex * uColor;
            }
        `;

        const vs = gl.createShader(gl.VERTEX_SHADER)!;
        gl.shaderSource(vs, vsSrc);
        gl.compileShader(vs);

        const fs = gl.createShader(gl.FRAGMENT_SHADER)!;
        gl.shaderSource(fs, fsSrc);
        gl.compileShader(fs);

        shaderProg = gl.createProgram()!;
        gl.attachShader(shaderProg, vs);
        gl.attachShader(shaderProg, fs);
        gl.linkProgram(shaderProg);
        gl.useProgram(shaderProg);

        aPos = gl.getAttribLocation(shaderProg, "aPosition");
        aUV = gl.getAttribLocation(shaderProg, "aTexCoord");
        uMat = gl.getUniformLocation(shaderProg, "uMatrix");
        uColor = gl.getUniformLocation(shaderProg, "uColor");
        uSamp = gl.getUniformLocation(shaderProg, "uSampler");

        gl.enable(gl.DEPTH_TEST);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

        // Character Geometries
        headBox = createGLBuffer(buildBox(-4, 12, -4, 8, 8, 8, 0, 0));
        hatBox = createGLBuffer(buildBox(-4.5, 11.5, -4.5, 9, 9, 9, 32, 0));
        torsoBox = createGLBuffer(buildBox(-4, 0, -2, 8, 12, 4, 16, 16));
        armRBox = createGLBuffer(buildBox(-8, 0, -2, 4, 12, 4, 40, 16));
        armLBox = createGLBuffer(buildBox(4, 0, -2, 4, 12, 4, 32, 48));
        legRBox = createGLBuffer(buildBox(-4, -12, -2, 4, 12, 4, 0, 16));
        legLBox = createGLBuffer(buildBox(0, -12, -2, 4, 12, 4, 16, 48));
        // Attached Flapping Cape (10w x 16h x 1d, 64x32 mapping)
        capeBox = createGLBuffer(buildBox(-5, -3, -2.8, 10, 16, 1, 0, 0, 64, 32));

        // Default Steve skin
        const cvSkin = document.createElement("canvas");
        cvSkin.width = 64; cvSkin.height = 64;
        const sctx = cvSkin.getContext("2d")!;
        sctx.fillStyle = "#5c4033"; sctx.fillRect(0, 0, 32, 16);
        sctx.fillStyle = "#c68642"; sctx.fillRect(8, 8, 8, 8);
        sctx.fillStyle = "#ffffff"; sctx.fillRect(8, 12, 2, 1); sctx.fillRect(14, 12, 2, 1);
        sctx.fillStyle = "#3c50a0"; sctx.fillRect(10, 12, 1, 1); sctx.fillRect(13, 12, 1, 1);
        sctx.fillStyle = "#a0522d"; sctx.fillRect(10, 14, 4, 1);
        sctx.fillStyle = "#00a8a8"; sctx.fillRect(16, 16, 24, 16);
        sctx.fillStyle = "#c68642"; sctx.fillRect(20, 16, 4, 4);
        sctx.fillStyle = "#00a8a8"; sctx.fillRect(40, 16, 16, 4);
        sctx.fillStyle = "#c68642"; sctx.fillRect(40, 20, 16, 12);
        sctx.fillStyle = "#00a8a8"; sctx.fillRect(32, 48, 16, 4);
        sctx.fillStyle = "#c68642"; sctx.fillRect(32, 52, 16, 12);
        sctx.fillStyle = "#2c3e50"; sctx.fillRect(0, 16, 16, 12);
        sctx.fillStyle = "#696969"; sctx.fillRect(0, 28, 16, 4);
        sctx.fillStyle = "#2c3e50"; sctx.fillRect(16, 48, 16, 12);
        sctx.fillStyle = "#696969"; sctx.fillRect(16, 60, 16, 4);

        skinTex = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, skinTex);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, cvSkin);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

        update3DCapeTexture();
        renderLoop();
    }

    function createGLBuffer(boxData: any) {
        if (!gl) return null;
        const pBuf = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, pBuf);
        gl.bufferData(gl.ARRAY_BUFFER, boxData.positions, gl.STATIC_DRAW);

        const uvBuf = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, uvBuf);
        gl.bufferData(gl.ARRAY_BUFFER, boxData.uvs, gl.STATIC_DRAW);

        return { pBuf, uvBuf, count: boxData.count };
    }

    function buildBox(x: number, y: number, z: number, w: number, h: number, d: number, u: number, v: number, tw = 64, th = 64) {
        const x0 = x, x1 = x + w, y0 = y, y1 = y + h, z0 = z, z1 = z + d;
        const u0 = u / tw, u1 = (u + d) / tw, u2 = (u + d + w) / tw, u3 = (u + d + w + d) / tw, u4 = (u + d + w + d + w) / tw;
        const v0 = v / th, v1 = (v + d) / th, v2 = (v + d + h) / th;

        const positions = [
            x0, y0, z1,  x1, y0, z1,  x1, y1, z1,   x0, y0, z1,  x1, y1, z1,  x0, y1, z1,
            x1, y0, z0,  x0, y0, z0,  x0, y1, z0,   x1, y0, z0,  x0, y1, z0,  x1, y1, z0,
            x0, y1, z1,  x1, y1, z1,  x1, y1, z0,   x0, y1, z1,  x1, y1, z0,  x0, y1, z0,
            x0, y0, z0,  x1, y0, z0,  x1, y0, z1,   x0, y0, z0,  x1, y0, z1,  x0, y0, z1,
            x1, y0, z1,  x1, y0, z0,  x1, y1, z0,   x1, y0, z1,  x1, y1, z0,  x1, y1, z1,
            x0, y0, z0,  x0, y0, z1,  x0, y1, z1,   x0, y0, z0,  x0, y1, z1,  x0, y1, z0
        ];
        const uvs = [
            u1, v2,  u2, v2,  u2, v1,   u1, v2,  u2, v1,  u1, v1,
            u3, v2,  u4, v2,  u4, v1,   u3, v2,  u4, v1,  u3, v1,
            u1, v1,  u2, v1,  u2, v0,   u1, v1,  u2, v0,  u1, v0,
            u2, v0,  u3, v0,  u3, v1,   u2, v0,  u3, v1,  u2, v1,
            u0, v2,  u1, v2,  u1, v1,   u0, v2,  u1, v1,  u0, v1,
            u2, v2,  u3, v2,  u3, v1,   u2, v2,  u3, v1,  u2, v1
        ];
        return { positions: new Float32Array(positions), uvs: new Float32Array(uvs), count: 36 };
    }

    function update3DCapeTexture() {
        if (!gl || !pixelCanvasEl) return;
        if (!capeTex) {
            capeTex = gl.createTexture();
        }
        gl.bindTexture(gl.TEXTURE_2D, capeTex);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, pixelCanvasEl);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    }

    function renderLoop() {
        if (gl && capeBox) {
            if (isFlapping) flapTime += 0.04;

            gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);
            gl.clearColor(0.04, 0.07, 0.13, 0);
            gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

            const aspect = gl.canvas.width / gl.canvas.height;
            const proj = mat4Perspective(mat4Identity(), 45 * Math.PI / 180, aspect, 1, 500);

            let view = mat4Identity();
            mat4Translate(view, view, 0, -4, -camDist);
            mat4RotateX(view, view, camRotX * Math.PI / 180);
            mat4RotateY(view, view, camRotY * Math.PI / 180);

            const viewProj = mat4Multiply(mat4Identity(), proj, view);

            const breath = isFlapping ? Math.sin(flapTime) * 0.04 : 0;
            const armSwing = isFlapping ? Math.sin(flapTime * 1.5) * 0.15 : 0;
            const capeFlap = isFlapping ? (Math.sin(flapTime * 2.5) * 0.18 + 0.12) : 0.08;

            if (skinTex) {
                let headMat = mat4Translate(mat4Identity(), viewProj, 0, breath * 3, 0);
                drawMesh(headBox, headMat, skinTex);
                drawMesh(hatBox, headMat, skinTex);
                drawMesh(torsoBox, viewProj, skinTex);

                let armRMat = mat4Translate(mat4Identity(), viewProj, 0, 10, 0);
                mat4RotateX(armRMat, armRMat, armSwing);
                mat4Translate(armRMat, armRMat, 0, -10, 0);
                drawMesh(armRBox, armRMat, skinTex);

                let armLMat = mat4Translate(mat4Identity(), viewProj, 0, 10, 0);
                mat4RotateX(armLMat, armLMat, -armSwing);
                mat4Translate(armLMat, armLMat, 0, -10, 0);
                drawMesh(armLBox, armLMat, skinTex);

                let legRMat = mat4Translate(mat4Identity(), viewProj, 0, 0, 0);
                mat4RotateX(legRMat, legRMat, -armSwing * 0.8);
                drawMesh(legRBox, legRMat, skinTex);

                let legLMat = mat4Translate(mat4Identity(), viewProj, 0, 0, 0);
                mat4RotateX(legLMat, legLMat, armSwing * 0.8);
                drawMesh(legLBox, legLMat, skinTex);
            }

            if (capeTex) {
                let capeMat = mat4Translate(mat4Identity(), viewProj, 0, 11, -2);
                mat4RotateX(capeMat, capeMat, -capeFlap * 2);
                mat4Translate(capeMat, capeMat, 0, -11, 2);
                drawMesh(capeBox, capeMat, capeTex);
            }

            // Project 3D nametag position
            const tag = document.getElementById("inGameCapeArtNametag");
            if (tag) {
                const rx = 19 * viewProj[4] + viewProj[12];
                const ry = 19 * viewProj[5] + viewProj[13];
                const rw = 19 * viewProj[7] + viewProj[15];
                if (rw > 0.1) {
                    const screenX = ((rx / rw) * 0.5 + 0.5) * gl.canvas.clientWidth;
                    const screenY = (1.0 - ((ry / rw) * 0.5 + 0.5)) * gl.canvas.clientHeight;
                    tag.style.left = `${screenX}px`;
                    tag.style.top = `${Math.max(10, screenY - 18)}px`;
                    tag.style.display = "block";
                }
            }
        }
        animFrame = requestAnimationFrame(renderLoop);
    }

    function drawMesh(mesh: any, matrix: Float32Array, texture: WebGLTexture) {
        if (!gl || !mesh || !texture) return;
        gl.bindBuffer(gl.ARRAY_BUFFER, mesh.pBuf);
        gl.enableVertexAttribArray(aPos);
        gl.vertexAttribPointer(aPos, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, mesh.uvBuf);
        gl.enableVertexAttribArray(aUV);
        gl.vertexAttribPointer(aUV, 2, gl.FLOAT, false, 0, 0);

        gl.uniformMatrix4fv(uMat, false, matrix);
        gl.uniform4fv(uColor, [1, 1, 1, 1]);

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.uniform1i(uSamp, 0);

        gl.drawArrays(gl.TRIANGLES, 0, mesh.count);
    }

    function mat4Identity() {
        return new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);
    }
    function mat4Perspective(out: Float32Array, fovy: number, aspect: number, near: number, far: number) {
        const f = 1.0 / Math.tan(fovy / 2);
        const nf = 1 / (near - far);
        out[0] = f / aspect; out[1] = 0; out[2] = 0; out[3] = 0;
        out[4] = 0; out[5] = f; out[6] = 0; out[7] = 0;
        out[8] = 0; out[9] = 0; out[10] = (far + near) * nf; out[11] = -1;
        out[12] = 0; out[13] = 0; out[14] = (2 * far * near) * nf; out[15] = 0;
        return out;
    }
    function mat4Multiply(out: Float32Array, a: Float32Array, b: Float32Array) {
        const a00 = a[0], a01 = a[1], a02 = a[2], a03 = a[3];
        const a10 = a[4], a11 = a[5], a12 = a[6], a13 = a[7];
        const a20 = a[8], a21 = a[9], a22 = a[10], a23 = a[11];
        const a30 = a[12], a31 = a[13], a32 = a[14], a33 = a[15];
        for (let i = 0; i < 4; i++) {
            const b0 = b[i * 4], b1 = b[i * 4 + 1], b2 = b[i * 4 + 2], b3 = b[i * 4 + 3];
            out[i * 4] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
            out[i * 4 + 1] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
            out[i * 4 + 2] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
            out[i * 4 + 3] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;
        }
        return out;
    }
    function mat4Translate(out: Float32Array, a: Float32Array, x: number, y: number, z: number) {
        out.set(a);
        out[12] = a[0] * x + a[4] * y + a[8] * z + a[12];
        out[13] = a[1] * x + a[5] * y + a[9] * z + a[13];
        out[14] = a[2] * x + a[6] * y + a[10] * z + a[14];
        out[15] = a[3] * x + a[7] * y + a[11] * z + a[15];
        return out;
    }
    function mat4RotateX(out: Float32Array, a: Float32Array, rad: number) {
        const s = Math.sin(rad), c = Math.cos(rad);
        const a10 = a[4], a11 = a[5], a12 = a[6], a13 = a[7];
        const a20 = a[8], a21 = a[9], a22 = a[10], a23 = a[11];
        out.set(a);
        out[4] = a10 * c + a20 * s; out[5] = a11 * c + a21 * s; out[6] = a12 * c + a22 * s; out[7] = a13 * c + a23 * s;
        out[8] = a20 * c - a10 * s; out[9] = a21 * c - a11 * s; out[10] = a22 * c - a12 * s; out[11] = a23 * c - a13 * s;
        return out;
    }
    function mat4RotateY(out: Float32Array, a: Float32Array, rad: number) {
        const s = Math.sin(rad), c = Math.cos(rad);
        const a00 = a[0], a01 = a[1], a02 = a[2], a03 = a[3];
        const a20 = a[8], a21 = a[9], a22 = a[10], a23 = a[11];
        out.set(a);
        out[0] = a00 * c - a20 * s; out[1] = a01 * c - a21 * s; out[2] = a02 * c - a22 * s; out[3] = a03 * c - a23 * s;
        out[8] = a00 * s + a20 * c; out[9] = a01 * s + a21 * c; out[10] = a02 * s + a22 * c; out[11] = a03 * s + a23 * c;
        return out;
    }

    function togglePreviewView() {
        if (previewView === "back") {
            previewView = "front";
            camRotX = 14;
            camRotY = -20;
        } else {
            previewView = "back";
            camRotX = 14;
            camRotY = 160;
        }
    }

    function resetPreviewCam() {
        camRotX = 14;
        camRotY = 160;
        camDist = 42;
        previewView = "back";
    }

    // ==========================================
    // Actions: Equip, Publish, Import, Export
    // ==========================================
    async function handleEquip() {
        if (!pixelCanvasEl) return;
        loading = true;
        try {
            const dataUrl = pixelCanvasEl.toDataURL("image/png");
            await equipClientCape(undefined, undefined, dataUrl);
            notification.set({
                title: "CapeArt Studio",
                message: "Custom cape equipped directly in-game and synced to server!",
                error: false
            });
            dispatch("equipped");
        } catch (e: any) {
            notification.set({
                title: "Equip Failed",
                message: `Could not equip cape: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    async function handlePublish() {
        if (!pixelCanvasEl) return;
        const title = prompt("Enter a title for your cape submission:", `${playerName}'s HD Cape`);
        if (!title) return;

        loading = true;
        try {
            const dataUrl = pixelCanvasEl.toDataURL("image/png");
            const cleanBase64 = dataUrl.substring(dataUrl.indexOf(",") + 1);
            // Submit to backend server
            const res = await fetch("/api/capes/submit", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    title: title.trim(),
                    author_name: playerName,
                    data_base64: cleanBase64,
                    is_hd: isHd ? 1 : 0
                })
            });
            const data = await res.json();
            if (data.success) {
                notification.set({
                    title: "Cape Submitted",
                    message: "Your cape has been published for staff review!",
                    error: false
                });
            } else {
                throw new Error(data.error || "Submission failed");
            }
        } catch (e: any) {
            notification.set({
                title: "Publish Failed",
                message: `Failed to publish cape: ${e.message || e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    function handleExportPNG() {
        if (!pixelCanvasEl) return;
        const a = document.createElement("a");
        a.download = `${playerName.toLowerCase()}_cape_${isHd ? "hd" : "std"}.png`;
        a.href = pixelCanvasEl.toDataURL("image/png");
        a.click();
    }

    function handleImportPNG(e: Event) {
        const input = e.target as HTMLInputElement;
        if (!input.files || input.files.length === 0) return;
        const file = input.files[0];
        const reader = new FileReader();
        reader.onload = (re) => {
            const img = new Image();
            img.onload = () => {
                if ((img.width === 64 && img.height === 32) || (img.width === 128 && img.height === 64)) {
                    setResolution(img.width === 128);
                    const ctx = pixelCanvasEl.getContext("2d", { willReadFrequently: true });
                    if (ctx) {
                        ctx.clearRect(0, 0, capeWidth, capeHeight);
                        ctx.drawImage(img, 0, 0, capeWidth, capeHeight);
                        saveUndo();
                        update3DCapeTexture();
                    }
                } else {
                    notification.set({
                        title: "Import Error",
                        message: `Invalid cape dimensions (${img.width}x${img.height}). Must be 64x32 or 128x64 PNG.`,
                        error: true
                    });
                }
            };
            img.src = re.target?.result as string;
        };
        reader.readAsDataURL(file);
    }
</script>

<div class="capeart-studio">
    <!-- Top Studio Toolbar -->
    <div class="studio-bar">
        <div class="tools-group">
            <button class="tool-btn" class:active={activeTool === "pencil"} on:click={() => activeTool = "pencil"} title="Pencil">✏️</button>
            <button class="tool-btn" class:active={activeTool === "eraser"} on:click={() => activeTool = "eraser"} title="Eraser">🧹</button>
            <button class="tool-btn" class:active={activeTool === "eyedropper"} on:click={() => activeTool = "eyedropper"} title="Eyedropper">👁️</button>
            <button class="tool-btn" class:active={activeTool === "bucket"} on:click={() => activeTool = "bucket"} title="Bucket Fill">🪣</button>
        </div>

        <div class="res-segmented">
            <button class="seg-btn" class:active={!isHd} on:click={() => setResolution(false)}>Std 64x32</button>
            <button class="seg-btn" class:active={isHd} on:click={() => setResolution(true)}>HD 128x64</button>
        </div>

        <div class="tools-group">
            <button class="tool-btn" on:click={undo} disabled={undoStack.length <= 1} title="Undo">↩️</button>
            <button class="tool-btn" on:click={clearCanvas} title="Clear Cape">🗑️</button>
            <button class="tool-btn" on:click={openCapeArtStudio} title="Open in Web Browser">🌐</button>
        </div>
    </div>

    <!-- Workspace: Left Pixel Grid, Right 3D Character Flap Preview -->
    <div class="studio-workspace">
        <div class="pixel-column">
            <div class="pixel-canvas-host">
                <canvas
                    bind:this={pixelCanvasEl}
                    width={capeWidth}
                    height={capeHeight}
                    class="pixel-canvas"
                    on:pointerdown={handlePointerDown}
                    on:pointermove={handlePointerMove}
                    on:pointerup={handlePointerUp}
                    on:contextmenu|preventDefault
                ></canvas>
            </div>

            <!-- Color Palette Swatches -->
            <div class="palette-bar">
                {#each PALETTE as color}
                    <button
                        class="swatch"
                        class:active={activeColor === color}
                        style="background: {color};"
                        on:click={() => activeColor = color}
                    ></button>
                {/each}
                <div class="color-picker-wrap">
                    <input type="color" bind:value={activeColor} class="color-picker-input">
                </div>
            </div>
        </div>

        <!-- Right: Real-time 3D Preview with Flapping Cape and Player Nametag -->
        <div class="preview-column">
            <div class="preview-box">
                <canvas bind:this={previewCanvasEl} width="240" height="250" class="gl-canvas"></canvas>
                <!-- In-Game Floating Nametag -->
                <div id="inGameCapeArtNametag" class="mc-nametag">{playerName}</div>
            </div>
            <div class="preview-controls">
                <button class="mini-btn" on:click={togglePreviewView}>
                    📷 {previewView === "back" ? "Skin View" : "Cape View"}
                </button>
                <button class="mini-btn" on:click={() => isFlapping = !isFlapping}>
                    {isFlapping ? "Pause" : "Flap"}
                </button>
                <button class="mini-btn" on:click={resetPreviewCam}>↺</button>
            </div>
        </div>
    </div>

    <!-- Bottom Actions Row -->
    <div class="studio-actions">
        <button class="action-btn equip-btn" on:click={handleEquip} disabled={loading}>
            ✓ Equip In-Game
        </button>
        <button class="action-btn publish-btn" on:click={handlePublish} disabled={loading}>
            ☁️ Publish to Catalog
        </button>
        <button class="action-btn sec-btn" on:click={handleExportPNG}>
            Export PNG
        </button>
        <label class="action-btn sec-btn file-btn">
            Import PNG
            <input type="file" accept="image/png" on:change={handleImportPNG} style="display: none;">
        </label>
    </div>
</div>

<style lang="scss">
    .capeart-studio {
        display: flex;
        flex-direction: column;
        gap: 12px;
        width: 100%;
    }

    .studio-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: rgba(0, 0, 0, 0.4);
        padding: 6px 10px;
        border-radius: 12px;
        border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .tools-group {
        display: flex;
        gap: 4px;
    }

    .tool-btn {
        background: transparent;
        border: 1px solid transparent;
        border-radius: 6px;
        padding: 4px 8px;
        font-size: 14px;
        cursor: pointer;
        color: #fff;
        transition: all 0.15s ease;

        &:hover {
            background: rgba(255, 255, 255, 0.1);
        }

        &.active {
            background: rgba(139, 92, 246, 0.3);
            border-color: #8b5cf6;
        }

        &:disabled {
            opacity: 0.3;
            cursor: not-allowed;
        }
    }

    .res-segmented {
        display: flex;
        background: rgba(0, 0, 0, 0.5);
        border-radius: 8px;
        padding: 2px;
        border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .seg-btn {
        background: transparent;
        border: none;
        color: rgba(255, 255, 255, 0.6);
        padding: 4px 10px;
        border-radius: 6px;
        font-size: 11px;
        font-weight: 600;
        cursor: pointer;

        &.active {
            background: #8b5cf6;
            color: #ffffff;
        }
    }

    .studio-workspace {
        display: grid;
        grid-template-columns: 1fr 240px;
        gap: 14px;
    }

    .pixel-column {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .pixel-canvas-host {
        background: radial-gradient(circle, #1a2234 0%, #0a0f18 100%);
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 10px;
        height: 250px;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.6);
    }

    .pixel-canvas {
        width: 100%;
        max-width: 440px;
        height: 220px;
        image-rendering: pixelated;
        cursor: crosshair;
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 4px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    }

    .palette-bar {
        display: flex;
        flex-wrap: wrap;
        gap: 5px;
        align-items: center;
        background: rgba(0, 0, 0, 0.3);
        padding: 6px;
        border-radius: 8px;
        border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .swatch {
        width: 20px;
        height: 20px;
        border-radius: 4px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        cursor: pointer;
        transition: transform 0.1s ease;

        &:hover {
            transform: scale(1.15);
        }

        &.active {
            border-color: #ffffff;
            transform: scale(1.18);
            box-shadow: 0 0 6px #fff;
        }
    }

    .color-picker-wrap {
        margin-left: auto;
    }

    .color-picker-input {
        width: 24px;
        height: 24px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 4px;
        background: transparent;
        cursor: pointer;
    }

    .preview-column {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .preview-box {
        position: relative;
        background: radial-gradient(circle, #172138 0%, #070b14 80%);
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 10px;
        height: 250px;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
    }

    .gl-canvas {
        width: 100%;
        height: 100%;
        cursor: grab;

        &:active {
            cursor: grabbing;
        }
    }

    .mc-nametag {
        position: absolute;
        top: 20px;
        left: 50%;
        transform: translate(-50%, -50%);
        background: rgba(0, 0, 0, 0.65);
        color: #ffffff;
        font-family: 'Minecraft', monospace;
        font-size: 11px;
        font-weight: 700;
        padding: 2px 8px;
        border-radius: 3px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        letter-spacing: 0.04em;
        pointer-events: none;
        z-index: 10;
        text-shadow: 1px 1px 0 #000;
        white-space: nowrap;
    }

    .preview-controls {
        display: flex;
        gap: 4px;
    }

    .mini-btn {
        flex: 1;
        background: rgba(255, 255, 255, 0.06);
        border: 1px solid rgba(255, 255, 255, 0.08);
        color: rgba(255, 255, 255, 0.8);
        border-radius: 6px;
        padding: 4px 6px;
        font-size: 11px;
        cursor: pointer;

        &:hover {
            background: rgba(255, 255, 255, 0.12);
            color: #fff;
        }
    }

    .studio-actions {
        display: flex;
        gap: 8px;
    }

    .action-btn {
        flex: 1;
        border: none;
        padding: 8px 12px;
        border-radius: 8px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.15s ease;

        &:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }
    }

    .equip-btn {
        background: #10b981;
        color: #ffffff;

        &:hover:not(:disabled) {
            background: #059669;
        }
    }

    .publish-btn {
        background: #8b5cf6;
        color: #ffffff;

        &:hover:not(:disabled) {
            background: #7c3aed;
        }
    }

    .sec-btn {
        background: rgba(255, 255, 255, 0.08);
        color: #ffffff;
        border: 1px solid rgba(255, 255, 255, 0.1);

        &:hover {
            background: rgba(255, 255, 255, 0.15);
        }
    }

    .file-btn {
        cursor: pointer;
    }
</style>
