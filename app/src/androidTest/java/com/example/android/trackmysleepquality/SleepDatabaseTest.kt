/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * This is not meant to be a full set of tests. For simplicity, most of your samples do not
 * include tests. However, when building the Room, it is helpful to make sure it works before
 * adding the UI.
 */

@RunWith(AndroidJUnit4::class)
class SleepDatabaseTest {

    private lateinit var sleepDao: SleepDatabaseDao
    private lateinit var db: SleepDatabase
    //Required to allow ObserveForever
    @Rule @JvmField
    val rule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, SleepDatabase::class.java)
                // Allowing main thread queries, just for testing.
                .allowMainThreadQueries()
                .build()
        sleepDao = db.sleepDatabaseDao
    }

    private fun populateDb(){

        val night = SleepNight()
        val night2 = SleepNight()
        val night3 = SleepNight()
        runBlocking{
            sleepDao.insert(night)
            sleepDao.insert(night2)
            sleepDao.insert(night3)
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetNight() {
        val night = SleepNight()
        runBlocking {
            sleepDao.insert(night)
        }
        val tonight: SleepNight?
        runBlocking{
            tonight = sleepDao.getTonight()
        }
        assertEquals(tonight?.sleepQuality, -1)
    }

    @Test
    @Throws(Exception::class)
    fun clearDb(){
        populateDb()
        runBlocking {
            sleepDao.clear()
        }
        val nightList : List<SleepNight>? = sleepDao.getAllNights().blockingObserve()
        assertEquals(nightList?.size, 0 )
    }

    @Test
    @Throws(Exception::class)
    fun updateNight(){

        val night = SleepNight()
        runBlocking {
            sleepDao.insert(night)
        }
        val retrievedNight: SleepNight?
        runBlocking {
            retrievedNight = sleepDao.getTonight()
        }

        assertEquals(retrievedNight?.sleepQuality, -1)
        retrievedNight?.sleepQuality = 5
        runBlocking {
            if (retrievedNight != null) {
                sleepDao.update(retrievedNight)
            }
        }
        val updatedNight : SleepNight?
        runBlocking {
            updatedNight = sleepDao.getTonight()
        }

        assertEquals(updatedNight?.sleepQuality, 5)
    }

    @Test
    @Throws(Exception::class)
    fun getNight(){
        populateDb()

        val retrievedNight: SleepNight?
        runBlocking {
            retrievedNight = sleepDao.get(2)
        }

        assertEquals(retrievedNight?.nightId, 2)
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllNights(){
        populateDb()
        val nightList : List<SleepNight>? = sleepDao.getAllNights().blockingObserve()

//        nightLiveData.observe(InstrumentationRegistry.getInstrumentation().targetContext, Observer {
//
//        } )
        //Log.i("testing", "${nightList}")
        assertEquals(nightList?.size, 3)
    }

    //Extension function for observing LiveData returned from Dao
    //found: https://stackoverflow.com/a/44991770/8049500
    private fun <T> LiveData<T>.blockingObserve(): T? {
        var value: T? = null
        val latch = CountDownLatch(1)

        val observer = Observer<T> { t ->
            value = t
            latch.countDown()
        }

        observeForever(observer)

        latch.await(2, TimeUnit.SECONDS)
        return value
    }
}

