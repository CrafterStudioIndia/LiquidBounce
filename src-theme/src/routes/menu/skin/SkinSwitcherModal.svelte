<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import {
        getClientSkins,
        selectClientSkin,
        setClientSkinModel,
        resetClientSkin,
        getClientCosmetics,
        setClientCosmetic,
        getClientCapes,
        equipClientCape,
        resetClientCape,
        openCapeArtStudio,
        getClientInfo,
        openFileDialog,
        type LocalSkinState,
        type LocalSkinItem,
        type ClientCosmeticsState,
        type ClientCapesState
    } from "../../../integration/rest";
    import { notification } from "../common/header/notification_store";
    import CapeArtStudio from "./CapeArtStudio.svelte";
    import { onMount } from "svelte";

    export let visible: boolean = false;

    let activeTab: "skins" | "cosmetics" | "capes" | "capeart" = "skins";

    let playerName: string = "Crafter_Studio";

    let skinState: LocalSkinState = {
        active: "",
        model: "classic",
        hasActive: false,
        skins: []
    };

    let capesState: ClientCapesState = {
        activeCapeId: "",
        hasActiveCape: false,
        activeCapeFile: ""
    };

    const CATALOG_CAPES = [
        { id: "1", name: "Liquid Gold", desc: "Classic 64x32 gilded client cape with crest", resolution: "Standard 64x32", color: "#f5be23" },
        { id: "cape-liquid", name: "Liquid Exclusive", desc: "Signature HD royal client cloak with Liquid crest", resolution: "HD 128x64", color: "#0ea5e9" },
        { id: "cape-neon-liquid", name: "Neon Liquid", desc: "Luminescent neon cyan cybernetic cloak", resolution: "HD 128x64", color: "#06b6d4" },
        { id: "2", name: "Cosmic Nebula", desc: "High Definition cosmic celestial interstellar cloak", resolution: "HD 128x64", color: "#8b5cf6" },
        { id: "3", name: "Corvus Midnight", desc: "High Definition midnight covert raven plumage", resolution: "HD 128x64", color: "#38bdf8" },
        { id: "4", name: "Emerald Matrix", desc: "High Definition cybernetic matrix crystal lattice", resolution: "HD 128x64", color: "#10b981" },
        { id: "cape-autumn-breeze", name: "Autumn Breeze", desc: "Golden foliage autumnal twilight cloak", resolution: "HD 128x64", color: "#f97316" },
        { id: "cape-angel-wings", name: "Angel Wings", desc: "Seraphic feathered angelic heraldic cape", resolution: "HD 128x64", color: "#e0e7ff" },
        { id: "cape-butterfly", name: "Monarch Butterfly", desc: "Vibrant stained glass butterfly wing cape", resolution: "HD 128x64", color: "#3b82f6" },
        { id: "cape-end-portal", name: "End Portal", desc: "Starry void interdimensional galactic cloak", resolution: "HD 128x64", color: "#a855f7" },
        { id: "cape-fire", name: "Inferno Flame", desc: "Raging magma volcanic blazing cape", resolution: "HD 128x64", color: "#ef4444" },
        { id: "cape-heart", name: "Floating Hearts", desc: "Charming sweet pastel heart constellation", resolution: "HD 128x64", color: "#ec4899" },
        { id: "cape-duck", name: "Duckling", desc: "Playful cheerful bright yellow duck cape", resolution: "Standard 64x32", color: "#eab308" }
    ];

    let cosmeticsState: ClientCosmeticsState = {
        aura: { style: "", primary: 0x9BE7FF, secondary: 0xFFFFFF, lift: 0, glow: 0.5 },
        wings: { style: "", primary: 0xF7F7FA, secondary: 0xC9D6EA, lift: 0, glow: 0.5 },
        headwear: { style: "", primary: 0xF2C14E, secondary: 0xD7263D, lift: 0, glow: 0.5 }
    };

    let selectedPet: string = "";
    let loading = false;

    const AURAS = [
        { id: "", name: "None", desc: "No aura effect" },
        { id: "aura-liquid-sigil", name: "Liquid Sigil", desc: "Signature Liquid cyan energy sigil" },
        { id: "aura-autumn-breeze", name: "Autumn Breeze", desc: "Swirling golden autumn motes" },
        { id: "aura-inferno", name: "Inferno Flame", desc: "Blazing cyclonic volcanic flames" },
        { id: "halo", name: "Crown Halo", desc: "Dual ring with diamond stars" },
        { id: "orbit", name: "Celestial Orbit", desc: "Quad-orbital precession motes" },
        { id: "purple_lightning", name: "Purple Lightning", desc: "Electric crackling storm aura" },
        { id: "portal", name: "End Portal", desc: "Interdimensional void vortex" },
        { id: "embers", name: "Volcanic Embers", desc: "Vortex rising cinder embers" },
        { id: "rings", name: "Planetary Rings", desc: "Gyroscopic planetary rings" },
        { id: "spiral", name: "Triple Spiral", desc: "Ascending triple-helix energy" },
        { id: "drift", name: "Ether Drift", desc: "Floating harmonic motes" },
        { id: "prism", name: "Spectral Prism", desc: "Prismatic floating crystals" },
        { id: "aura-heart", name: "Floating Hearts", desc: "Ascending harmonic heart particles" },
        { id: "aura-broken-hearts", name: "Broken Hearts", desc: "Abyssal shattered heart drift" },
        { id: "aura-blood-rain", name: "Blood Rain", desc: "Crimson atmospheric precipitation" },
        { id: "aura-bat", name: "Bat Swarm", desc: "Nocturnal swirling shadow bats" },
        { id: "aura-bubble", name: "Bubble Motes", desc: "Aquatic buoyant air spheres" },
        { id: "aura-fish", name: "Fish Swarm", desc: "Deep sea orbital swimming fish" },
        { id: "aura-candy", name: "Sweet Candy", desc: "Confectionery festive drift" },
        { id: "aura-candle", name: "Mystic Candle", desc: "Enchanted levitating candlelight" },
        { id: "aura-halloween-particle", name: "Halloween", desc: "Spooky spectral phantom motes" }
    ];

    const WINGS = [
        { id: "", name: "None", desc: "No wings" },
        { id: "golden-wings", name: "Golden Wings", desc: "Gilded celestial seraph plumage" },
        { id: "wings-autumn-breeze", name: "Autumn Breeze", desc: "Radiant amber breeze wings" },
        { id: "wings-angel", name: "Angel Wings", desc: "Multi-tiered grand feathered wings" },
        { id: "wings-baby-neon-dragon", name: "Baby Neon Dragon", desc: "Luminescent cyan cyber drake wings" },
        { id: "wings-baby-white-dragon", name: "Baby White Dragon", desc: "Pure pearl white dragon wings" },
        { id: "wings-fiery-dragon", name: "Fiery Dragon", desc: "Incandescent blaze dragon wings" },
        { id: "wings-ghost-dragon", name: "Ghost Dragon", desc: "Ethereal translucent phantom wings" },
        { id: "wings-pumpkin", name: "Pumpkin Wings", desc: "Halloween amber carved wings" },
        { id: "wings-skeleton", name: "Skeleton Wings", desc: "Chiseled articulated bone wings" },
        { id: "wings-butterfly", name: "Monarch Butterfly", desc: "Elongated stained-glass monarch wings" },
        { id: "phoenix", name: "Solar Phoenix", desc: "Solar flame-tipped feathers" },
        { id: "demon", name: "Abyssal Demon", desc: "Barbed abyssal membrane" },
        { id: "dragon", name: "Ender Drake", desc: "Massive articulated dragon wings" },
        { id: "raven", name: "Corvus Raven", desc: "Midnight dark covert plumage" },
        { id: "bat", name: "Nocturnal Bat", desc: "Nocturnal leather membrane" },
        { id: "fairy", name: "Ether Fairy", desc: "Gossamer lattice fairy wings" },
        { id: "crystal", name: "Prismatic Crystal", desc: "Prismatic chiseled crystal blades" },
        { id: "mech", name: "Cyber Mech", desc: "Hydraulic thrust vectoring fins" }
    ];

    const HEADWEAR = [
        { id: "", name: "None", desc: "No hat" },
        { id: "hat-liquid-cap", name: "Liquid Cap", desc: "Signature Liquid curved brim cap" },
        { id: "hat-liquid-headband", name: "Liquid Headband", desc: "Athletic cyan Liquid sigil band" },
        { id: "golden-halo", name: "Golden Halo", desc: "Floating gilded celestial halo" },
        { id: "crown", name: "Royal Crown", desc: "Gilded four-point royal crown" },
        { id: "hat-victorian-crown", name: "Victorian Crown", desc: "Ornate silver filigree tiara" },
        { id: "hat-fire-crown", name: "Fire Crown", desc: "Flame-crested solar coronal crown" },
        { id: "hat-goat-horns", name: "Goat Horns", desc: "Curved ivory highland goat horns" },
        { id: "hat-shadow-horns", name: "Shadow Horns", desc: "Midnight demonic shadow horns" },
        { id: "horns", name: "Demon Horns", desc: "Curved crimson demon horns" },
        { id: "tophat", name: "Top Hat", desc: "Formal velvet top hat with satin band" },
        { id: "hat-monocle", name: "Monocle", desc: "Aristocratic gilded glass eye-piece" },
        { id: "hat-bucket", name: "Bucket Hat", desc: "Streetwear stitched canvas bucket hat" },
        { id: "hat-straw", name: "Straw Hat", desc: "Woven sun farmer straw hat" },
        { id: "hat-bunny", name: "Bunny Ears", desc: "Soft pastel bunny ear headband" },
        { id: "hat-pumpkin-head", name: "Pumpkin Head", desc: "Carved jack-o'-lantern headwear" },
        { id: "beanie", name: "Warm Beanie", desc: "Ribbed knit beanie with pom-pom" },
        { id: "hat-orange-wool", name: "Orange Wool", desc: "Warm spun orange wool toque" },
        { id: "hat-fiery-headband", name: "Fiery Headband", desc: "Blazing flame ember headband" },
        { id: "hat-butterfly-halo", name: "Butterfly Halo", desc: "Fluttering celestial butterfly aura" },
        { id: "wizard", name: "Wizard Hat", desc: "Mystic pointed starry hat" },
        { id: "witch_hat", name: "Witch Hat", desc: "Midnight violet brewmasters hat" },
        { id: "santa_hat", name: "Santa Hat", desc: "Crimson velvet holiday fleece cap" },
        { id: "cat_ears", name: "Cat Ears", desc: "Anime feline ears with pink lining" }
    ];

    const PETS = [
        { id: "", name: "None", desc: "No companion pet" },
        { id: "pet-dragon", name: "Baby Dragon", desc: "Miniature hovering dragon companion" },
        { id: "pet-black-cat", name: "Black Cat", desc: "Playful sleek midnight feline" },
        { id: "pet-butterfly", name: "Butterfly", desc: "Graceful floating monarch butterfly" },
        { id: "pet-crow", name: "Crow", desc: "Intelligent sentinel raven companion" },
        { id: "pet-duck", name: "Duck", desc: "Cheerful yellow waterfowl pet" },
        { id: "pet-duckling", name: "Duckling", desc: "Tiny buoyant baby duckling" },
        { id: "pet-fish", name: "Swimming Fish", desc: "Aquatic hovering tropical fish" },
        { id: "pet-ghost", name: "Spooky Ghost", desc: "Gentle floating ethereal phantom" },
        { id: "pet-herobrine", name: "Mini Herobrine", desc: "Legendary white-eyed mini avatar" },
        { id: "pet-koala", name: "Koala", desc: "Cozy eucalyptus branch companion" },
        { id: "pet-shark", name: "Shark", desc: "Fierce hovering aerial shark" },
        { id: "pet-skeleton", name: "Skeleton", desc: "Chiseled articulated skeleton buddy" }
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

        try {
            capesState = await getClientCapes();
        } catch (e) {
            console.error("Failed to fetch capes:", e);
        }
    }

    async function handleEquipCape(capeId: string, capeName: string) {
        loading = true;
        try {
            await equipClientCape(capeId);
            await loadData();
            notification.set({
                title: "Cape Wardrobe",
                message: `Equipped cape: ${capeName}`,
                error: false
            });
        } catch (e) {
            notification.set({
                title: "Cape Wardrobe",
                message: `Failed to equip cape: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    async function handleUploadCape() {
        try {
            const result = await openFileDialog({
                mode: "SingleFile",
                supportedExtensions: ["png"]
            });
            if (result && result.file) {
                loading = true;
                await equipClientCape(undefined, result.file);
                await loadData();
                notification.set({
                    title: "Cape Wardrobe",
                    message: "Custom cape uploaded and equipped successfully!",
                    error: false
                });
            }
        } catch (e) {
            notification.set({
                title: "Cape Wardrobe",
                message: `Failed to open cape: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    async function handleResetCape() {
        loading = true;
        try {
            await resetClientCape();
            await loadData();
            notification.set({
                title: "Cape Wardrobe",
                message: "Unequipped client cape",
                error: false
            });
        } catch (e) {
            notification.set({
                title: "Cape Wardrobe",
                message: `Failed to reset cape: ${e}`,
                error: true
            });
        } finally {
            loading = false;
        }
    }

    function handleOpenCapeArt() {
        activeTab = "capeart";
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

    async function updatePet(styleId: string) {
        selectedPet = styleId;
        try {
            await setClientCosmetic("pet" as any, styleId, 0x0EA5E9, 0xFFFFFF, 0, 0.5);
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
    <div class="studio-wrapper" class:wide={activeTab === "capeart"}>
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
            <button
                class="tab-pill"
                class:active={activeTab === "capes"}
                on:click={() => activeTab = "capes"}
            >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
                Cape Menu
            </button>
            <button
                class="tab-pill"
                class:active={activeTab === "capeart"}
                on:click={() => activeTab = "capeart"}
            >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9"/>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                CapeArt Studio
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
        {:else if activeTab === "cosmetics"}
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

                <!-- Companion Pets Section -->
                <div class="cosmetic-group">
                    <div class="group-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                        </svg>
                        Companion Pets
                    </div>
                    <div class="items-grid">
                        {#each PETS as pet (pet.id)}
                            <button
                                class="cosmetic-card"
                                class:selected={selectedPet === pet.id}
                                on:click={() => updatePet(pet.id)}
                            >
                                <div class="card-name">{pet.name}</div>
                                <div class="card-desc">{pet.desc}</div>
                            </button>
                        {/each}
                    </div>
                </div>
            </div>
        {:else if activeTab === "capes"}
            <div class="capes-content">
                <!-- User cape profile banner -->
                <div class="profile-header">
                    <div class="profile-title">{playerName}</div>
                    <div class="profile-status">
                        <span class="status-dot"></span>
                        {#if capesState.hasActiveCape}
                            <span>Cape Active: {capesState.activeCapeId ? `Catalog ID #${capesState.activeCapeId}` : "Custom Loaded Cape"}</span>
                        {:else}
                            <span>Vanilla / No Cape</span>
                        {/if}
                    </div>
                </div>

                <!-- Controls row: Launch CapeArt Studio | Upload Cape... | Reset -->
                <div class="section-label">Studio & Management</div>
                <div class="controls-bar">
                    <button class="pill-btn editor-pill" on:click={handleOpenCapeArt}>
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 20h9"/>
                            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                        </svg>
                        🎨 Launch CapeArt Studio
                    </button>

                    <button class="pill-btn upload-pill" on:click={handleUploadCape} disabled={loading}>
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="17 8 12 3 7 8"/>
                            <line x1="12" y1="3" x2="12" y2="15"/>
                        </svg>
                        Upload Cape (.png)
                    </button>

                    <button class="pill-btn reset-pill" on:click={handleResetCape} disabled={loading || !capesState.hasActiveCape}>
                        Reset
                    </button>
                </div>

                <!-- Featured / Approved Capes Catalog -->
                <div class="section-label">Reviewed & Approved Catalog ({CATALOG_CAPES.length})</div>
                <div class="capes-grid">
                    {#each CATALOG_CAPES as cape (cape.id)}
                        <div class="cape-card" class:active={capesState.activeCapeId === cape.id}>
                            <div class="cape-preview" style="background: radial-gradient(circle, {cape.color}33 0%, rgba(15, 23, 42, 0.8) 100%); border-color: {cape.color}66;">
                                <div class="cape-silhouette" style="background: {cape.color};">
                                    <div class="cape-crest"></div>
                                </div>
                            </div>
                            <div class="cape-info">
                                <div class="cape-header">
                                    <span class="cape-title">{cape.name}</span>
                                    <span class="cape-badge" style="border-color: {cape.color}; color: {cape.color};">{cape.resolution}</span>
                                </div>
                                <div class="cape-desc">{cape.desc}</div>
                                <div class="cape-actions">
                                    {#if capesState.activeCapeId === cape.id}
                                        <button class="equip-btn active" disabled>
                                            ✓ Equipped
                                        </button>
                                    {:else}
                                        <button class="equip-btn" on:click={() => handleEquipCape(cape.id, cape.name)} disabled={loading}>
                                            Equip Cape
                                        </button>
                                    {/if}
                                </div>
                            </div>
                        </div>
                    {/each}
                </div>
            </div>
        {:else if activeTab === "capeart"}
            <CapeArtStudio {playerName} on:equipped={loadData}/>
        {/if}
    </div>
</Modal>

<style lang="scss">
    .studio-wrapper {
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-width: 880px;
        max-width: 980px;
        transition: max-width 0.2s ease, min-width 0.2s ease;

        &.wide {
            min-width: 980px;
            max-width: 1080px;
        }
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
        gap: 18px;
        max-height: 560px;
        overflow-y: auto;
        padding-right: 6px;
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
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
        gap: 10px;
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
    .editor-pill {
        background: linear-gradient(135deg, rgba(139, 92, 246, 0.35) 0%, rgba(59, 130, 246, 0.35) 100%);
        border: 1px solid rgba(139, 92, 246, 0.5);
        color: #c4b5fd;
        font-weight: 600;

        &:hover {
            background: linear-gradient(135deg, rgba(139, 92, 246, 0.5) 0%, rgba(59, 130, 246, 0.5) 100%);
            color: #ffffff;
            box-shadow: 0 0 12px rgba(139, 92, 246, 0.4);
        }
    }

    .capes-content {
        display: flex;
        flex-direction: column;
        gap: 14px;
        max-height: 560px;
        overflow-y: auto;
        padding-right: 6px;
    }

    .capes-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
    }

    .cape-card {
        display: flex;
        gap: 12px;
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 10px;
        padding: 10px;
        transition: all 0.2s ease;

        &:hover {
            background: rgba(255, 255, 255, 0.06);
            border-color: rgba(255, 255, 255, 0.16);
            transform: translateY(-1px);
        }

        &.active {
            border-color: #3b82f6;
            background: rgba(59, 130, 246, 0.12);
            box-shadow: 0 0 12px rgba(59, 130, 246, 0.25);
        }
    }

    .cape-preview {
        width: 54px;
        height: 80px;
        border-radius: 6px;
        border: 1px solid rgba(255, 255, 255, 0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
    }

    .cape-silhouette {
        width: 32px;
        height: 52px;
        border-radius: 4px;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .cape-crest {
        width: 10px;
        height: 14px;
        background: rgba(255, 255, 255, 0.85);
        border-radius: 2px;
    }

    .cape-info {
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        flex: 1;
        gap: 4px;
        min-width: 0;
    }

    .cape-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 4px;
    }

    .cape-title {
        font-size: 13px;
        font-weight: 600;
        color: #ffffff;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .cape-badge {
        font-size: 9px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        padding: 2px 5px;
        border-radius: 4px;
        border: 1px solid;
        flex-shrink: 0;
    }

    .cape-desc {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.5);
        line-height: 1.3;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .cape-actions {
        margin-top: 4px;
    }

    .equip-btn {
        width: 100%;
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: #ffffff;
        padding: 5px 10px;
        border-radius: 6px;
        font-size: 11px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover:not(:disabled) {
            background: #2563eb;
            border-color: #3b82f6;
            box-shadow: 0 0 10px rgba(59, 130, 246, 0.4);
        }

        &.active {
            background: rgba(34, 197, 94, 0.2);
            border-color: #22c55e;
            color: #86efac;
            cursor: default;
        }

        &:disabled {
            opacity: 0.8;
        }
    }
</style>
