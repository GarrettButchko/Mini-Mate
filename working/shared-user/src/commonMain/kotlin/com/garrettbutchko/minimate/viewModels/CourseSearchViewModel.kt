package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.MapRegionData
import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.interfaces.LocationFinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CourseSearchViewModel(
    private val locationHandler: LocationFinding,
    private val courseViewModel: CourseViewModel
) {
    private val _mapCameraPosition = MutableStateFlow<MapRegionData?>(null)
    val mapCameraPosition: StateFlow<MapRegionData?> = _mapCameraPosition.asStateFlow()

    private val _selectedMapItem = MutableStateFlow<MapItemDTO?>(null)
    val selectedMapItem: StateFlow<MapItemDTO?> = _selectedMapItem.asStateFlow()

    private val _mapItems = MutableStateFlow<List<MapItemDTO>>(emptyList())
    val mapItems: StateFlow<List<MapItemDTO>> = _mapItems.asStateFlow()

    private val _nameExists = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val nameExists: StateFlow<Map<String, Boolean>> = _nameExists.asStateFlow()

    private val _isSearchPanelVisible = MutableStateFlow(false)
    val isSearchPanelVisible: StateFlow<Boolean> = _isSearchPanelVisible.asStateFlow()

    private val _hasLocationAccess = MutableStateFlow(false)
    val hasLocationAccess: StateFlow<Boolean> = _hasLocationAccess.asStateFlow()

    // Setters for Map and Item State
    fun setMapCameraPosition(position: MapRegionData?) {
        // Send to courseViewModel so it remains the source of truth
        if (position != null) {
            courseViewModel.setPosition(position)
        }
    }

    fun setNewMapPosition() {
        // Use the selected item if one exists, otherwise calculate based on all items
        val newPosition = locationHandler.updateCameraRegion()
        if (newPosition != null) {
            courseViewModel.setPosition(newPosition)
        }
    }

    fun setMapItems(items: List<MapItemDTO>) {
        _mapItems.value = items
    }

    fun setSelectedMapItem(item: MapItemDTO?) {
        if (_selectedMapItem.value != item) {
            _selectedMapItem.value = item
            locationHandler.setSelectedItem(item)
            val newPosition = locationHandler.updateCameraRegion()
            if (newPosition != null) {
                courseViewModel.setPosition(newPosition)
            }
        }
    }

    // Setters for Logic and UI State
    fun setNameExists(nameMap: Map<String, Boolean>) {
        _nameExists.value = nameMap
    }

    fun setSearchPanelVisible(isVisible: Boolean) {
        _isSearchPanelVisible.value = isVisible
    }

    fun setLocationAccess(hasAccess: Boolean) {
        _hasLocationAccess.value = hasAccess
    }


    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        setupSubscriptions()
    }

    private fun setupSubscriptions() {
        // --- Sync from LocationHandler to this ViewModel ---
        locationHandler.mapItems.onEach { items ->
            _mapItems.value = items
            courseViewModel.preloadNameChecks(items)
        }.launchIn(scope)

        locationHandler.hasLocationAccess.onEach { access ->
            _hasLocationAccess.value = access
        }.launchIn(scope)

        // --- Sync from the original CourseViewModel to this ViewModel ---
        courseViewModel.isUpperHalf.onEach { isUpperHalf ->
            _isSearchPanelVisible.value = isUpperHalf
        }.launchIn(scope)

        courseViewModel.position.onEach { position ->
            _mapCameraPosition.value = position
        }.launchIn(scope)

        courseViewModel.nameExists.onEach { exists ->
            _nameExists.value = exists
        }.launchIn(scope)

        // --- Sync map item selection ---
        locationHandler.selectedItem.onEach { item ->
            if (_selectedMapItem.value != item) {
                _selectedMapItem.value = item
            }
        }.launchIn(scope)
    }



    fun onAppear() {
        courseViewModel.onAppearance()
    }

    fun recenterMap() {
        val newPosition = locationHandler.updateCameraRegion()
        if (newPosition != null) {
            courseViewModel.setPosition(newPosition)
        }
    }
}
