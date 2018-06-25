package org.fossasia.openevent.general.search

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.text.TextUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.fossasia.openevent.general.data.Preference
import org.fossasia.openevent.general.event.Event
import org.fossasia.openevent.general.event.EventService
import timber.log.Timber

class SearchViewModel(private val eventService: EventService, private val preference: Preference) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private val tokenKey = "LOCATION"

    val progress = MutableLiveData<Boolean>()
    val events = MutableLiveData<List<Event>>()
    val error = MutableLiveData<String>()
    var searchEvent: String? = null
    val savedLocation by lazy { preference.getString(tokenKey) }

    fun loadEvents(location: String) {
        preference.putString(tokenKey, location)
        val query: String = if (TextUtils.isEmpty(location))
            "[{\"name\":\"name\",\"op\":\"ilike\",\"val\":\"%$searchEvent%\"}]"
        else
            "[{\"and\":[{\"name\":\"location-name\",\"op\":\"ilike\",\"val\":\"%$location%\"},{\"name\":\"name\",\"op\":\"ilike\",\"val\":\"%$searchEvent%\"}]}]"

        compositeDisposable.add(eventService.getSearchEvents(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe({
                    progress.value = true
                }).doFinally({
                    progress.value = false
                }).subscribe({
                    events.value = it
                }, {
                    Timber.e(it, "Error fetching events")
                    error.value = "Error fetching events"
                }))
    }

    fun setFavorite(eventId: Long, favourite: Boolean) {
        compositeDisposable.add(eventService.setFavorite(eventId, favourite)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Successfully added %d to favorites", eventId)
                }, {
                    Timber.e(it, "Error adding %d to favorites", eventId)
                    error.value = "Error adding to favorites"
                }))
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}