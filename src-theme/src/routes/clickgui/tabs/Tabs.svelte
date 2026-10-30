<script lang="ts">
    import type { Component } from "svelte";

    type Tab = {
        title: string;
        content: Component;
    };

    let {
        tabs,
        activeTab = $bindable(0),
        onOpenCosmetics
    } = $props<{
        tabs: Tab[];
        activeTab?: number;
        onOpenCosmetics?: () => void;
    }>();

    const Active = $derived(tabs[activeTab]?.content);
</script>

<div class="tabs">
    <div class="available-tabs">
        {#each tabs as tab, index (tab.title)}
            <button
                    class="tab-button"
                    class:active={index === activeTab}
                    onclick={() => (activeTab = index)}
                    type="button"
            >
                {tab.title}
            </button>
        {/each}

        {#if onOpenCosmetics}
            <button
                    class="tab-button cosmetics-pill-btn"
                    onclick={onOpenCosmetics}
                    type="button"
                    title="Open Cosmetics & Wardrobe"
            >
                <svg class="cosmetics-pill-icon" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                    <path d="M12 2L14.2 8.3L20.5 10.5L14.2 12.7L12 19L9.8 12.7L3.5 10.5L9.8 8.3L12 2Z"/>
                </svg>
                <span>Cosmetics</span>
            </button>
        {/if}
    </div>

    <div class="content">
        {#if Active}
            {@render Active()}
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .available-tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 5px;
    padding: 6px;
    border-radius: 999px;
    background-color: rgba($clickgui-base-color, 0.85);
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    z-index: 9999999999;
  }

  .tab-button {
    background: transparent;
    color: $clickgui-text-dimmed-color;
    padding: 6px 14px;
    font-size: 14px;
    font-weight: 600;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color 0.2s, ease color 0.2s;
    border: solid 1px transparent;

    &:hover {
      color: $clickgui-text-color;
      background-color: rgba($clickgui-base-color, 0.7);
    }

    &.active {
      color: $clickgui-text-color;
      background-color: rgba($accent-color, 0.15);
      border: 1px solid rgba($accent-color, 0.8);
    }

    &.cosmetics-pill-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: #93c5fd;
      border: 1px solid rgba(59, 130, 246, 0.4);
      background-color: rgba(59, 130, 246, 0.12);

      &:hover {
        color: #ffffff;
        background-color: rgba(59, 130, 246, 0.28);
        border-color: rgba(59, 130, 246, 0.8);
      }

      .cosmetics-pill-icon {
        color: #60a5fa;
      }
    }
  }
</style>
