<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import {
        getClientSkins,
        selectClientSkin,
        setClientSkinModel,
        resetClientSkin,
        getClientCosmetics,
        setClientCosmetic,
        getClientInfo,
        openFileDialog,
        type LocalSkinState,
        type LocalSkinItem,
        type ClientCosmeticsState
    } from "../../../integration/rest";
    import { notification } from "../common/header/notification_store";
    import { onMount } from "svelte";

    export let visible: boolean = false;

    let activeTab: "skins" | "cosmetics" = "skins";

    let playerName: string = "Crafter_Studio";

    let skinState: LocalSkinState = {
        active: "",
        model: "classic",
        hasActive: false,
        skins: []
    };

    let cosmeticsState: ClientCosmeticsState = {
        aura: { style: "", primary: 0x9BE7FF, secondary: 0xFFFFFF, lift: 0, glow: 0.5 },
        wings: { style: "", primary: 0xF7F7FA, secondary: 0xC9D6EA, lift: 0, glow: 0.5 },
        headwear: { style: "", primary: 0xF2C14E, secondary: 0xD7263D, lift: 0, glow: 0.5 }
    };

    let loading = false;

    const AURAS = [
        { id: "", name: "None", desc: "No aura" },
        { id: "orbit", name: "Orbit", desc: "Quad-orbital precession motes" },
        { id: "halo", name: "Crown Halo", desc: "Dual ring with diamond stars" },
        { id: "embers", name: "Embers", desc: "Vortex cyclonic rising embers" },
        { id: "rings", name: "Planetary Rings", desc: "Gyroscopic planetary rings" },
        { id: "spiral", name: "Triple Spiral", desc: "Ascending triple-helix energy" },
        { id: "drift", name: "Ether Drift", desc: "Floating harmonic motes" },
        { id: "prism", name: "Prism", desc: "Spectral floating crystals" }
    ];

    const WINGS = [
        { id: "", name: "None", desc: "No wings" },
        { id: "angel", name: "Archangel", desc: "Multi-tiered grand feathered wings" },
        { id: "raven", name: "Corvus", desc: "Midnight dark covert plumage" },
        { id: "phoenix", name: "Phoenix", desc: "Solar flame-tipped feathers" },
        { id: "demon", name: "Demon", desc: "Barbed abyssal membrane" },
        { id: "dragon", name: "Ender Drake", desc: "Massive articulated dragon wings" },
        { id: "bat", name: "Bat", desc: "Nocturnal leather membrane" },
        { id: "butterfly", name: "Swallowtail", desc: "Elongated monarch tails" },
        { id: "fairy", name: "Fairy", desc: "Gossamer lattice leaf wings" },
        { id: "crystal", name: "Crystal", desc: "Prismatic chiseled crystal blades" },
        { id: "mech", name: "Cyber Mech", desc: "Hydraulic thrust vectoring fins" }
    ];

    const HEADWEAR = [
        { id: "", name: "None", desc: "No hat" },
        { id: "tophat", name: "Top Hat", desc: "Formal velvet hat with satin band" },
        { id: "crown", name: "Royal Crown", desc: "Gilded four-point royal crown" },
        { id: "beanie", name: "Beanie", desc: "Ribbed knit beanie with pom-pom" },
        { id: "horns", name: "Demon Horns", desc: "Curved demon horns with tips" },
        { id: "wizard", name: "Wizard Hat", desc: "Mystic pointed starry hat" }
    ];

    async function loadData() {
        try {
            const info = await getClientInfo();
            if (info?.username) {
                playerName = info.username;
            }
        } catch (_) {}

        try {
            skinState = await getClientSkins();
        } catch (e) {
            console.error("Failed to fetch skins:", e);
        }

        try {
            cosmeticsState = await getClientCosmetics();
        } catch (e) {
            console.error("Failed to fetch cosmetics:", e);
        }
    }

    $: if (visible) {
        loadData();
    }

    async function handleSelectSkin(skin: LocalSkinItem) {
        loading = true;
        try {
            await selectClientSkin(skin.file, skin.model);
            await loadData();
            notification.set({
                title: "Skin Studio",
                message: `Applied skin: ${skin.name}`,
                error: false
            });
        } catch (e) {
            notification.set({
                title: "Skin Studio",
                message: `Failed to set skin: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    async function handleUploadSkin() {
        try {
            const result = await openFileDialog({
                mode: "SingleFile",
                supportedExtensions: ["png"]
            });
            if (result && result.file) {
                loading = true;
                await selectClientSkin(result.file, skinState.model || "classic");
                await loadData();
                notification.set({
                    title: "Skin Studio",
                    message: "Skin uploaded and applied successfully!",
                    error: false
                });
            }
        } catch (e) {
            notification.set({
                title: "Skin Studio",
                message: `Failed to open skin: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    async function handleSetModel(model: "classic" | "slim") {
        try {
            await setClientSkinModel(model);
            skinState.model = model;
            notification.set({
                title: "Skin Studio",
                message: `Model set to ${model === "slim" ? "Slim (3px)" : "Classic (4px)"}`,
                error: false
            });
        } catch (e) {
            notification.set({
                title: "Skin Studio",
                message: `Failed to set model: ${e}`,
                error: true
            });
        }
    }

    async function handleReset() {
        try {
            await resetClientSkin();
            await loadData();
            notification.set({
                title: "Skin Studio",
                message: "Reset skin to default Mojang texture",
                error: false
            });
        } catch (e) {
            notification.set({
                title: "Skin Studio",
                message: `Failed to reset skin: ${e}`,
                error: true
            });
        }
    }

    async function updateAura(styleId: string) {
        cosmeticsState.aura.style = styleId;
        try {
            await setClientCosmetic("aura", styleId, cosmeticsState.aura.primary, cosmeticsState.aura.secondary, cosmeticsState.aura.lift, cosmeticsState.aura.glow);
        } catch (e) {
            console.error(e);
        }
    }

    async function updateWings(styleId: string) {
        cosmeticsState.wings.style = styleId;
        try {
            await setClientCosmetic("wings", styleId, cosmeticsState.wings.primary, cosmeticsState.wings.secondary, cosmeticsState.wings.lift, cosmeticsState.wings.glow);
        } catch (e) {
            console.error(e);
        }
    }

    async function updateHeadwear(styleId: string) {
        cosmeticsState.headwear.style = styleId;
        try {
            await setClientCosmetic("headwear", styleId, cosmeticsState.headwear.primary, cosmeticsState.headwear.secondary, cosmeticsState.headwear.lift, cosmeticsState.headwear.glow);
        } catch (e) {
            console.error(e);
        }
    }

    function hexToNumber(hex: string): number {
        return parseInt(hex.replace("#", ""), 16) || 0;
    }

    function numberToHex(num: number): string {
        return "#" + (num & 0xFFFFFF).toString(16).padStart(6, "0");
    }
</script>

<Modal title="Studio & Wardrobe" bind:visible={visible}>
    <div class="studio-wrapper">
        <!-- Studio Tab Switcher -->
        <div class="tabs-header">
            <button
                class="tab-pill"
                class:active={activeTab === "skins"}
                on:click={() => activeTab = "skins"}
            >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.38 3.46L16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"/>
                </svg>
                Skin Studio
            </button>
            <button
                class="tab-pill"
                class:active={activeTab === "cosmetics"}
                on:click={() => activeTab = "cosmetics"}
            >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                Cosmetics & Auras
            </button>
        </div>

        {#if activeTab === "skins"}
            <div class="skins-content">
                <!-- User profile banner matching screenshot -->
                <div class="profile-header">
                    <div class="profile-title">{playerName}</div>
                    <div class="profile-status">
                        <span class="status-dot"></span>
                        {#if skinState.hasActive}
                            <span>Custom Skin Active ({skinState.model === "slim" ? "Slim 3px" : "Classic 4px"})</span>
                        {:else}
                            <span>Vanilla Mojang Texture</span>
                        {/if}
                    </div>
                </div>

                <!-- Controls row matching exact screenshot layout: Upload Skin... | Classic | Slim | Reset -->
                <div class="section-label">Skin</div>
                <div class="controls-bar">
                    <button class="pill-btn upload-pill" on:click={handleUploadSkin} disabled={loading}>
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="17 8 12 3 7 8"/>
                            <line x1="12" y1="3" x2="12" y2="15"/>
                        </svg>
                        Upload Skin...
                    </button>

                    <div class="segmented-pill">
                        <button
                            class="segment-btn"
                            class:active={skinState.model === "classic"}
                            on:click={() => handleSetModel("classic")}
                        >
                            Classic
                        </button>
                        <button
                            class="segment-btn"
                            class:active={skinState.model === "slim"}
                            on:click={() => handleSetModel("slim")}
                        >
                            Slim
                        </button>
                    </div>

                    <button class="pill-btn reset-pill" on:click={handleReset} disabled={loading}>
                        Reset
                    </button>
                </div>

                <!-- Saved skins section -->
                <div class="section-label">Saved ({skinState.skins.length})</div>

                {#if skinState.skins.length > 0}
                    <div class="skins-row">
                        {#each skinState.skins as skin (skin.id)}
                            <button
                                type="button"
                                class="skin-figure-card"
                                on:click={() => handleSelectSkin(skin)}
                            >
                                <div class="figure-silhouette">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                                        <path d="M20.38 3.46L16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"/>
                                    </svg>
                                </div>
                                <div class="figure-name" title={skin.name}>{skin.name}</div>
                                <span class="figure-tag">{skin.model}</span>
                            </button>
                        {/each}
                    </div>
                {:else}
                    <div class="empty-state">
                        <p>No saved skins found in <code>.minecraft/skins/</code></p>
                        <span class="empty-hint">Use <strong>Upload Skin...</strong> above to import your skin file.</span>
                    </div>
                {/if}
            </div>
        {:else}
            <!-- Cosmetics Studio -->
            <div class="cosmetics-content">
                <!-- Aura Section -->
                <div class="cosmetic-group">
                    <div class="group-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"/>
                            <circle cx="12" cy="12" r="4"/>
                        </svg>
                        Aura Effect
                    </div>
                    <div class="items-grid">
                        {#each AURAS as aura (aura.id)}
                            <button
                                class="cosmetic-card"
                                class:selected={cosmeticsState.aura.style === aura.id}
                                on:click={() => updateAura(aura.id)}
                            >
                                <div class="card-name">{aura.name}</div>
                                <div class="card-desc">{aura.desc}</div>
                            </button>
                        {/each}
                    </div>
                </div>

                <!-- Wings Section -->
                <div class="cosmetic-group">
                    <div class="group-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 2L2 9l10 13 10-13-10-7z"/>
                        </svg>
                        Wings
                    </div>
                    <div class="items-grid">
                        {#each WINGS as wing (wing.id)}
                            <button
                                class="cosmetic-card"
                                class:selected={cosmeticsState.wings.style === wing.id}
                                on:click={() => updateWings(wing.id)}
                            >
                                <div class="card-name">{wing.name}</div>
                                <div class="card-desc">{wing.desc}</div>
                            </button>
                        {/each}
                    </div>
                </div>

                <!-- Headwear Section -->
                <div class="cosmetic-group">
                    <div class="group-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polygon points="12 2 15 8 22 9 17 14 18 21 12 18 6 21 7 14 2 9 9 8 12 2"/>
                        </svg>
                        Headwear
                    </div>
                    <div class="items-grid">
                        {#each HEADWEAR as hat (hat.id)}
                            <button
                                class="cosmetic-card"
                                class:selected={cosmeticsState.headwear.style === hat.id}
                                on:click={() => updateHeadwear(hat.id)}
                            >
                                <div class="card-name">{hat.name}</div>
                                <div class="card-desc">{hat.desc}</div>
                            </button>
                        {/each}
                    </div>
                </div>
            </div>
        {/if}
    </div>
</Modal>

<style lang="scss">
    .studio-wrapper {
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-width: 520px;
        max-width: 600px;
    }

    .tabs-header {
        display: flex;
        gap: 8px;
        background: rgba(0, 0, 0, 0.3);
        padding: 4px;
        border-radius: 20px;
        border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .tab-pill {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        background: transparent;
        border: none;
        color: rgba(255, 255, 255, 0.6);
        padding: 8px 16px;
        border-radius: 16px;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.18s ease;

        &:hover {
            color: #ffffff;
            background: rgba(255, 255, 255, 0.06);
        }

        &.active {
            color: #ffffff;
            background: rgba(255, 255, 255, 0.15);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
        }
    }

    .skins-content {
        display: flex;
        flex-direction: column;
        gap: 14px;
    }

    .profile-header {
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 10px 0 4px 0;
    }

    .profile-title {
        font-size: 26px;
        font-weight: 700;
        color: #ffffff;
        letter-spacing: -0.5px;
    }

    .profile-status {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: rgba(255, 255, 255, 0.65);

        .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #22c55e;
            box-shadow: 0 0 6px rgba(34, 197, 94, 0.6);
        }
    }

    .section-label {
        font-size: 13px;
        font-weight: 600;
        color: rgba(255, 255, 255, 0.5);
        text-transform: uppercase;
        letter-spacing: 0.5px;
        margin-top: 4px;
    }

    .controls-bar {
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .pill-btn {
        display: flex;
        align-items: center;
        gap: 6px;
        border: none;
        border-radius: 20px;
        padding: 8px 18px;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease;

        &:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }
    }

    .upload-pill {
        background: rgba(255, 255, 255, 0.88);
        color: #121214;

        &:hover:not(:disabled) {
            background: #ffffff;
            box-shadow: 0 2px 10px rgba(255, 255, 255, 0.25);
            transform: translateY(-1px);
        }
    }

    .reset-pill {
        background: rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.85);

        &:hover:not(:disabled) {
            background: rgba(239, 68, 68, 0.25);
            color: #ef4444;
        }
    }

    .segmented-pill {
        display: flex;
        background: rgba(0, 0, 0, 0.35);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 20px;
        padding: 3px;
    }

    .segment-btn {
        background: transparent;
        border: none;
        color: rgba(255, 255, 255, 0.65);
        padding: 5px 14px;
        border-radius: 16px;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
            color: #ffffff;
        }

        &.active {
            background: #2563eb;
            color: #ffffff;
            box-shadow: 0 1px 6px rgba(37, 99, 235, 0.4);
        }
    }

    .skins-row {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        max-height: 240px;
        overflow-y: auto;
        padding: 4px 2px;
    }

    .skin-figure-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.09);
        border-radius: 12px;
        padding: 12px 10px;
        width: 88px;
        cursor: pointer;
        transition: all 0.18s ease;

        &:hover {
            background: rgba(255, 255, 255, 0.12);
            border-color: rgba(255, 255, 255, 0.25);
            transform: translateY(-2px);
        }

        .figure-silhouette {
            width: 44px;
            height: 44px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: rgba(0, 0, 0, 0.35);
            border-radius: 8px;
            color: rgba(255, 255, 255, 0.7);
        }

        .figure-name {
            font-size: 11px;
            font-weight: 500;
            color: #ffffff;
            max-width: 74px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .figure-tag {
            font-size: 9px;
            text-transform: uppercase;
            color: rgba(255, 255, 255, 0.4);
            letter-spacing: 0.5px;
        }
    }

    .cosmetics-content {
        display: flex;
        flex-direction: column;
        gap: 16px;
        max-height: 380px;
        overflow-y: auto;
        padding-right: 4px;
    }

    .cosmetic-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .group-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
        color: rgba(255, 255, 255, 0.8);
    }

    .items-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
        gap: 8px;
    }

    .cosmetic-card {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 3px;
        background: rgba(255, 255, 255, 0.04);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 8px;
        padding: 8px 10px;
        text-align: left;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
            background: rgba(255, 255, 255, 0.09);
            border-color: rgba(255, 255, 255, 0.2);
        }

        &.selected {
            background: rgba(37, 99, 235, 0.2);
            border-color: #3b82f6;
            box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);

            .card-name {
                color: #60a5fa;
            }
        }

        .card-name {
            font-size: 12px;
            font-weight: 600;
            color: #ffffff;
        }

        .card-desc {
            font-size: 10px;
            color: rgba(255, 255, 255, 0.45);
            line-height: 1.2;
        }
    }

    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 24px;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 8px;
        border: 1px dashed rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.6);
        font-size: 12px;

        p {
            margin: 0 0 4px 0;
        }

        .empty-hint {
            color: rgba(255, 255, 255, 0.4);
            font-size: 11px;
        }
    }
</style>
