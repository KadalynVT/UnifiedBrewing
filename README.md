
# Unified Brewing

Unifies the brewing cauldrons from [Iron's Spells & Spellbooks][1], [Hexalia][2], and [\[Let's Do\] HerbalBrews][3].

Currently only for NeoForge 1.21.1 because the rest of the mods (roughly) are.

[1]: https://github.com/iron431/irons-spells-n-spellbooks
[2]: https://github.com/AstralyaStudios/Hexalia
[3]: https://github.com/Let-s-Do-Collection/HerbalBrews

## General changes

* Potions now stack to 4 (configurable).

## Brewing Stand

From vanilla Minecraft

* Can infuse Herbal Infusion into water bottle(s) to make Mundane Herbal Tea.
* Mundane potions produced will track the ingredient used to produce them.
* Added an extra output slot to the interface for balance with cauldrons.

## Alchemist Cauldron changes

From Iron's Spells & Spellbooks

* Can restrict brewing of Brewing Stand recipes to still require Blaze Powder.
  * Blaze-touched can be added by right-clicking with a Blazing Ladle (1 charge touches all fluid in the cauldron).
  * You can also add a Depleted Blaze Powder to the cauldron instead, which depletes one charge per ingredient being used for a vanilla mix.
* Allows for brewing Small Cauldron recipes by leaving the ingredients floating on top of 1000 mB of water (it's okay if any mix into the water) then using a ladle on the cauldron.
  * By default, it takes the same amount of time to cook. It will produce smoke particles during this time.
  * Must be collected in Rustic Bottles, 500 mB per bottle.
* Allows for mixing vanilla potions from mundane potions which were brewed from ingredients which make a potion. For example, an awkward potion and a mundane potion brewed from a spider eye. The cauldron must be full, of the used mundanes and water as filler, then use a ladle on the cauldron to mix it.
  * Iron's must be configured to allow vanilla brewing.
* Can empty Flasks into the cauldron and fill flasks from it. This splits the flask into its components plus a Mundane Herbal Tea (representing the Herbal Infusion).
* Can brew Mundane Herbal Tea by floating it in water.

## Small Cauldron changes

From Hexalia

* Add `unifiedbrewing:ladle` tag. Any item tagged as such can be used for mixing.
* Prevents lighting yourself on fire if you try to light a Small Cauldron which is already lit.
* Able to extinguish by right-clicking with a shovel.
* Can brew Brewing Stand recipes (configurable) or recipes from other cauldrons. Stirring is still required.
* Can restrict brewing of Brewing Stand recipes to still require Blaze Powder by requiring recipes which do to be stirred with a Blazing Ladle.
* Can input potions of all kinds to use as the base.
  * If HerbalBrews is present, it allows inserting mixes of different types of potions. You can extract the individual potions with bottles same as the Alchemist Cauldron, but you must add a Mundane Herbal Tea (or mix in an Herbal Infusion) in order to extract the combination with a Flask. Only 3 potions can be mixed into a Flask, if you stuffed 4 into the cauldron, one will be left behind.
  * If Irons is present, this also includes ink and blood.
* Configuration to disable inserting items which are not used for making potions.
* Configuration to disallow mixing non-functional recipes.
* Configuration to disable the poison AoE.

Note that currently Small Cauldron provides no method of collecting blood.

## Brewing Cauldron changes

From \[Let's Do] HerbalBrews

* Added Empty Flasks and recipes for them.
* Empty Flasks are required to be placed in the output slot before brewing starts now.
* Flask stack size limited to 4, down from 16 (configurable).
* Can collect blood by placing Glass Bottles in the output slot and standing on top (or pushing a different creature on top).
* Can brew Brewing Stand recipes (configurable) or recipes from other cauldrons by inserting one of the ingredients into the fuel slot first.
* Can restrict brewing of Brewing Stand recipes to still require Blaze Powder by adding a Depleted Blaze Powder to the fuel slot for those recipes.
* Can brew recipes specifically made for the Brewing Cauldron, which are defined but unused in the original mod.

## Other mod compatibility

* If Alex's Mobs is installed, adds a recipe to craft Blood Sac + 3 Glass Bottles into 3 Blood Vials.
* If Biomes o' Plenty is installed, adds collection and insertion of Blood Buckets to the Alchemist and Small Cauldrons.

## TODO

* Alchemist Cauldron not saving cooking state

* better view of empty tanks in Jade for Alchemist Cauldron
* view fluid contents in Jade for Small Cauldron

* don't allow collecting "Mixed Potion" from AC with normal bottle?

* add transparency to empty flask icons
* color overlays for flasks?
