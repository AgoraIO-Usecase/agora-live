package io.agora.scene.show.beauty

/**
 * Beauty cache
 *
 * @constructor Create empty Beauty cache
 */
object BeautyCache {

    private val defaultItemValueMap = mapOf(
        Pair(ITEM_ID_BEAUTY_SMOOTH, 0.7f),
        Pair(ITEM_ID_BEAUTY_REDDEN, 0.3f),
        Pair(ITEM_ID_BEAUTY_VSHAPE, 0.5f),
        Pair(ITEM_ID_BEAUTY_CHIN, 0.3f),
        Pair(ITEM_ID_BEAUTY_FOREHEAD, 0.3f),
        Pair(ITEM_ID_BEAUTY_EYE, 0.4f),
        Pair(ITEM_ID_BEAUTY_EYE_DISTANCE, 0.5f),
        Pair(ITEM_ID_BEAUTY_BROW_POSITION, 0.5f),
        Pair(ITEM_ID_BEAUTY_BROW_THICKNESS, 0.5f),
        Pair(ITEM_ID_BEAUTY_NOSE, 0.5f),
        Pair(ITEM_ID_BEAUTY_MOUTH_POSITION, 0.5f),
        Pair(ITEM_ID_BEAUTY_NOSE_LIFT, 0.5f),
        Pair(ITEM_ID_BEAUTY_MOUTH, 0.4f),
        Pair(ITEM_ID_BEAUTY_THICK_LIPS, 0.5f),

        Pair(ITEM_ID_EFFECT_SEXY, 0.69f),
        Pair(ITEM_ID_EFFECT_TIANMEI, 0.69f),
    )
    // key: itemId
    private val cacheItemValueMap = mutableMapOf<Int, Float>()
    private val cacheItemOperation = mutableMapOf<Int, ArrayList<Int>>()

    init {
        reset()
    }

    /**
     * Get last operation item id
     *
     * @param groupId
     * @return
     */
    fun getLastOperationItemId(groupId: Int): Int {
        return cacheItemOperation[groupId]?.lastOrNull() ?: groupId
    }

    /**
     * Get item value with default
     *
     * @param itemId
     * @return
     */
    fun getItemValueWithDefault(itemId: Int): Float {
        var value = cacheItemValueMap[itemId]
        if (value != null) {
            return value
        }
        value = defaultItemValueMap[itemId]
        if (value != null) {
            return value
        }
        return getItemValue(itemId)
    }

    /**
     * Get item value
     *
     * @param itemId
     * @return
     */
    fun getItemValue(itemId: Int): Float {
        val value = cacheItemValueMap[itemId]
        if (value != null) {
            return value
        }
        return when (itemId) {
            // Some effect zero in 0.5
            ITEM_ID_VIRTUAL_BG_GREEN_SCREENSTRENGTH,
            ITEM_ID_BEAUTY_CHIN,
            ITEM_ID_BEAUTY_FOREHEAD,
            ITEM_ID_BEAUTY_EYE_DISTANCE,
            ITEM_ID_BEAUTY_BROW_POSITION,
            ITEM_ID_BEAUTY_BROW_THICKNESS,
            ITEM_ID_BEAUTY_MOUTH_POSITION,
            ITEM_ID_BEAUTY_NOSE_LIFT,
            ITEM_ID_BEAUTY_MOUTH,
            ITEM_ID_BEAUTY_THICK_LIPS,
            -> 0.5f

            else -> 0.0f
        }
    }

    /**
     * Reset
     *
     */
    internal fun reset(){
        cacheItemValueMap.apply {
            clear()
            put(ITEM_ID_BEAUTY_SMOOTH, defaultItemValueMap[ITEM_ID_BEAUTY_SMOOTH] ?: 0.0f)
            put(ITEM_ID_BEAUTY_REDDEN, defaultItemValueMap[ITEM_ID_BEAUTY_REDDEN] ?: 0.0f)
            put(ITEM_ID_BEAUTY_VSHAPE, defaultItemValueMap[ITEM_ID_BEAUTY_VSHAPE] ?: 0.0f)
            put(ITEM_ID_BEAUTY_CHIN, defaultItemValueMap[ITEM_ID_BEAUTY_CHIN] ?: 0.0f)
            put(ITEM_ID_BEAUTY_FOREHEAD, defaultItemValueMap[ITEM_ID_BEAUTY_FOREHEAD] ?: 0.0f)
            put(ITEM_ID_BEAUTY_EYE, defaultItemValueMap[ITEM_ID_BEAUTY_EYE] ?: 0.0f)
            put(ITEM_ID_BEAUTY_EYE_DISTANCE, defaultItemValueMap[ITEM_ID_BEAUTY_EYE_DISTANCE] ?: 0.0f)
            put(ITEM_ID_BEAUTY_BROW_POSITION, defaultItemValueMap[ITEM_ID_BEAUTY_BROW_POSITION] ?: 0.0f)
            put(ITEM_ID_BEAUTY_BROW_THICKNESS, defaultItemValueMap[ITEM_ID_BEAUTY_BROW_THICKNESS] ?: 0.0f)
            put(ITEM_ID_BEAUTY_NOSE, defaultItemValueMap[ITEM_ID_BEAUTY_NOSE] ?: 0.0f)
            put(ITEM_ID_BEAUTY_MOUTH_POSITION, defaultItemValueMap[ITEM_ID_BEAUTY_MOUTH_POSITION] ?: 0.0f)
            put(ITEM_ID_BEAUTY_NOSE_LIFT, defaultItemValueMap[ITEM_ID_BEAUTY_NOSE_LIFT] ?: 0.0f)
            put(ITEM_ID_BEAUTY_MOUTH, defaultItemValueMap[ITEM_ID_BEAUTY_MOUTH] ?: 0.0f)
            put(ITEM_ID_BEAUTY_THICK_LIPS, defaultItemValueMap[ITEM_ID_BEAUTY_THICK_LIPS] ?: 0.0f)

            put(ITEM_ID_EFFECT_SEXY, defaultItemValueMap[ITEM_ID_BEAUTY_THICK_LIPS] ?: 0.0f)
            put(ITEM_ID_EFFECT_TIANMEI, defaultItemValueMap[ITEM_ID_BEAUTY_THICK_LIPS] ?: 0.0f)
        }
        cacheItemOperation.apply {
            clear()
            put(
                GROUP_ID_BEAUTY, arrayListOf(
                    ITEM_ID_BEAUTY_SMOOTH
                )
            )
            put(
                GROUP_ID_AR, arrayListOf(
                    ITEM_ID_AR_NONE
                )
            )
            put(
                GROUP_ID_STICKER, arrayListOf(
                    ITEM_ID_STICKER_NONE
                )
            )
            put(
                GROUP_ID_EFFECT, arrayListOf(
                    ITEM_ID_EFFECT_NONE
                )
            )
        }
    }

    internal fun resetGroupValue(groupId: Int){
        cacheItemValueMap.keys.toList().forEach {
            if ((it and groupId) > 0) {
                cacheItemValueMap.remove(it)
            }
        }
    }

    internal fun resetGroupOperation(groupId: Int){
        cacheItemOperation.keys.toList().forEach {
            if ((it and groupId) > 0) {
                cacheItemOperation.remove(it)
            }
        }
    }

    internal fun cacheItemValue(groupId: Int, itemId: Int, value: Float) {
        if (itemId == groupId) {
            cacheItemValueMap.keys.toList().forEach {
                if ((it and groupId) > 0) {
                    cacheItemValueMap.remove(it)
                }
            }
            return
        }
        cacheItemValueMap[itemId] = value
    }

    internal fun cacheOperation(groupId: Int, itemId: Int) {
        if (itemId == groupId) {
            cacheItemOperation.remove(groupId)
            return
        }
        cacheItemOperation[groupId] = arrayListOf<Int>().apply {
            cacheItemOperation[groupId]?.let {
                it.remove(itemId)
                addAll(it)
            }
            add(itemId)
        }
    }

    internal fun restoreByOperation(processor: IBeautyProcessor) {
        cacheItemOperation[GROUP_ID_BEAUTY]?.let { list ->
            list.lastOrNull()?.let {
                processor.setFaceBeautify(it, getItemValue(it))
            }
        }
        cacheItemOperation[GROUP_ID_EFFECT]?.let { list ->
            list.lastOrNull()?.let {
                processor.setEffect(it, getItemValue(it))
            }
        }
        cacheItemOperation[GROUP_ID_ADJUST]?.let { list ->
            list.lastOrNull()?.let {
                processor.setAdjust(it, getItemValue(it))
            }
        }
        cacheItemOperation[GROUP_ID_STICKER]?.let { list ->
            list.lastOrNull()?.let {
                processor.setSticker(it)
            }
        }
        cacheItemOperation[GROUP_ID_AR]?.let { list ->
            list.lastOrNull()?.let {
                processor.setARMark(it)
            }
        }
    }
}