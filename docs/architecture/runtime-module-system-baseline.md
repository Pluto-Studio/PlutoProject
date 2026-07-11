# Runtime Module System Baseline

This snapshot records the legacy runtime feature system before the runtime module
system refactor. It is intended for migration comparison, not as a specification
of the target architecture.

## Build baseline

- Date: 2026-07-11
- Commit: `3179fece` (`refactor/runtime-module-system`)
- `./gradlew test`: passed (`BUILD SUCCESSFUL`, 169 actionable tasks)
- `./gradlew shadowJar`: passed (`BUILD SUCCESSFUL`, 125 actionable tasks)
- Unified artifact: `build/libs/plutoproject-1.6.10.jar` (5.1 MiB)
- Pre-existing failures: none observed
- Pre-existing warnings:
  - Gradle reports that type-safe project accessors are incubating.
  - The `build-logic` root project has no explicit name, which may affect cache keys across checkout paths.
  - Gradle native-platform calls a restricted JDK method without `--enable-native-access=ALL-UNNAMED`.

Both commands were run without `clean`; most tasks were up-to-date. The successful
`shadowJar` invocation executed the final packaging task and the artifact was
inspected after the build.

## Packaged legacy manifests

The unified JAR contains these generated legacy manifests:

```text
META-INF/plutoproject/features/paper/_feature-paper.json
META-INF/plutoproject/features/paper/_feature_gallery_adapter-paper.json
META-INF/plutoproject/features/paper/_feature_whitelist-v2_adapter-paper.json
META-INF/plutoproject/features/velocity/_feature-velocity.json
META-INF/plutoproject/features/velocity/_feature_whitelist-v2_adapter-velocity.json
```

It also contains `paper-plugin.yml`, `velocity-plugin.json`,
`paper-dependencies.txt`, and `velocity-dependencies.txt`.

## Paper feature manifest

| ID | Entrypoint | Declared dependencies |
| --- | --- | --- |
| `afk` | `plutoproject.feature.paper.afk.AfkFeature` | — |
| `align` | `plutoproject.feature.paper.align.AlignFeature` | — |
| `back` | `plutoproject.feature.paper.back.BackFeature` | `teleport` required |
| `creeper_firework` | `plutoproject.feature.paper.creeperFirework.CreeperFireworkFeature` | — |
| `daily` | `plutoproject.feature.paper.daily.DailyFeature` | `menu` optional |
| `dev_watermark` | `plutoproject.feature.paper.devWatermark.DevWatermarkFeature` | — |
| `dynamic_scheduler` | `plutoproject.feature.paper.dynamicScheduler.DynamicSchedulerFeature` | `menu` optional |
| `elevator` | `plutoproject.feature.paper.elevator.ElevatorFeature` | — |
| `exchange_shop` | `plutoproject.feature.paper.exchangeshop.ExchangeShopFeature` | `menu` optional |
| `farm_protection` | `plutoproject.feature.paper.farmProtection.FarmProtectionFeature` | — |
| `gallery` | `plutoproject.feature.gallery.adapter.paper.GalleryFeature` | `menu` optional |
| `gm` | `plutoproject.feature.paper.gm.GmFeature` | — |
| `hat` | `plutoproject.feature.paper.hat.HatFeature` | — |
| `head` | `plutoproject.feature.paper.head.HeadFeature` | — |
| `home` | `plutoproject.feature.paper.home.HomeFeature` | `teleport` required; `menu` optional |
| `itemframe_protection` | `plutoproject.feature.paper.itemFrameProtection.ItemFrameProtectionFeature` | `gallery` optional |
| `lectern_protection` | `plutoproject.feature.paper.lecternProtection.LecternProtectionFeature` | — |
| `menu` | `plutoproject.feature.paper.menu.MenuFeature` | — |
| `no_creeper_block_breaks` | `plutoproject.feature.paper.noCreeperBlockBreaks.NoCreeperBlockBreaksFeature` | — |
| `no_join_quit_message` | `plutoproject.feature.paper.noJoinQuitMessage.NoJoinQuitMessageFeature` | — |
| `no_player_cap` | `plutoproject.feature.paper.noplayercap.NoPlayerCapFeature` | — |
| `overload_warning` | `plutoproject.feature.paper.overloadWarning.OverloadWarningFeature` | — |
| `pvp_toggle` | `plutoproject.feature.paper.pvpToggle.PvPToggleFeature` | — |
| `random_teleport` | `plutoproject.feature.paper.randomTeleport.RandomTeleportFeature` | `teleport` required; `menu` optional |
| `recipe` | `plutoproject.feature.paper.recipe.RecipeFeature` | — |
| `recipe_unlock` | `plutoproject.feature.paper.recipeunlock.RecipeUnlockFeature` | — |
| `server_selector` | `plutoproject.feature.paper.serverSelector.ServerSelectorFeature` | `menu` optional |
| `sit` | `plutoproject.feature.paper.sit.SitFeature` | — |
| `status` | `plutoproject.feature.paper.status.StatusFeature` | — |
| `suicide` | `plutoproject.feature.paper.suicide.SuicideFeature` | — |
| `teleport` | `plutoproject.feature.paper.teleport.TeleportFeature` | `menu` optional |
| `warp` | `plutoproject.feature.paper.warp.WarpFeature` | `teleport` required; `menu` optional |
| `whitelist_v2` | `plutoproject.feature.whitelist_v2.adapter.paper.WhitelistFeature` | `warp` optional |

## Velocity feature manifest

| ID | Entrypoint | Declared dependencies |
| --- | --- | --- |
| `join_quit_message` | `plutoproject.feature.velocity.joinQuitMessage.JoinQuitMessageFeature` | — |
| `motd` | `plutoproject.feature.velocity.motd.MotdFeature` | — |
| `player_cap` | `plutoproject.feature.velocity.playercap.PlayerCap` | — |
| `server_selector` | `plutoproject.feature.velocity.serverSelector.ServerSelectorFeature` | — |
| `version_checker` | `plutoproject.feature.velocity.versionchecker.VersionChecker` | — |
| `whitelist` | `plutoproject.feature.velocity.whitelist.Whitelist` | — |
| `whitelist_v2` | `plutoproject.feature.whitelist_v2.adapter.velocity.WhitelistFeature` | — |

## Legacy dependency graph

All legacy dependency declarations use `Load.BEFORE`. An arrow points from the
consumer to its declared dependency.

```text
back                 --required--> teleport
home                 --required--> teleport
random_teleport      --required--> teleport
warp                 --required--> teleport

daily                --optional--> menu
dynamic_scheduler    --optional--> menu
exchange_shop        --optional--> menu
gallery              --optional--> menu
home                 --optional--> menu
random_teleport      --optional--> menu
server_selector      --optional--> menu
teleport             --optional--> menu
warp                 --optional--> menu
itemframe_protection --optional--> gallery
whitelist_v2         --optional--> warp
```

The legacy manifests do not describe framework services such as MongoDB,
CharonFlow, GeoIP, interactive listeners, server statistics, or world aliases.
Those implicit runtime dependencies must be identified while capabilities and
their consumers are migrated.
