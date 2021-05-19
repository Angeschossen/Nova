package xyz.xenondevs.nova.ability

import com.google.gson.JsonElement
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.fromJson
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*

private val ABILITIES_KEY = NamespacedKey(NOVA, "abilities")

object AbilityManager : Listener {
    
    private val activeAbilities = HashMap<Player, EnumMap<AbilityType, Ability>>()
    private var tick = 0
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach(::handlePlayerJoin)
        runTaskTimer(0, 1) {
            activeAbilities.values.flatMap { it.values }.forEach { it.handleTick(tick) }
            tick++
        }
        NOVA.disableHandlers.add { Bukkit.getOnlinePlayers().forEach(::handlePlayerQuit) }
    }
    
    fun giveAbility(player: Player, type: AbilityType) {
        val ability = type.constructor.invoke(player)
        getAbilityMap(player)[type] = ability
        updateAbilityData(player)
    }
    
    fun takeAbility(player: Player, type: AbilityType) {
        val abilityMap = getAbilityMap(player)
        val ability = abilityMap[type]
        ability?.handleRemove()
        abilityMap.remove(type)
        updateAbilityData(player)
    }
    
    private fun getAbilityMap(player: Player): EnumMap<AbilityType, Ability> {
        return activeAbilities[player]
            ?: emptyEnumMap<AbilityType, Ability>().also { activeAbilities[player] = it }
    }
    
    private fun updateAbilityData(player: Player) {
        val dataContainer = player.persistentDataContainer
        val element = GSON.toJsonTree(getAbilityMap(player).keys)
        dataContainer.set(ABILITIES_KEY, element)
    }
    
    private fun handlePlayerQuit(player: Player) {
        getAbilityMap(player).values.forEach(Ability::handleRemove)
        activeAbilities.remove(player)
    }
    
    private fun handlePlayerJoin(player: Player) {
        val dataContainer = player.persistentDataContainer
        val element: JsonElement? = dataContainer.get(ABILITIES_KEY)
        if (element != null) {
            val abilityTypeList: List<AbilityType> = GSON.fromJson(element)!!
            abilityTypeList.forEach { giveAbility(player, it) }
        }
    }
    
    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) =
        handlePlayerQuit(event.player)
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) =
        handlePlayerJoin(event.player)
    
    enum class AbilityType(internal val constructor: (Player) -> Ability) {
        
        JETPACK(::JetpackFlyAbility)
        
    }
    
}