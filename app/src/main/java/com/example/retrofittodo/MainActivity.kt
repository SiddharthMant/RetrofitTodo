package com.example.retrofittodo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.retrofittodo.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var dropdownAdapter: ArrayAdapter<User>
    private lateinit var users: MutableList<User>
    private lateinit var todos: MutableList<Todo>
    var updateJob : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data
        lifecycleScope.launchWhenCreated {
            binding.progressBar.isVisible = true

            val todosDeferred = async { RetrofitInstance.api.getTodos(null) }
            val usersDeferred = async { RetrofitInstance.api.getUsers() }

            val todoResponse = try {
                todosDeferred.await()
            } catch (e: IOException) {
                // Most likely no internet connection or maybe output stream closed
                Log.e(TAG, "IOException, you might not have internet connection")
                binding.progressBar.isVisible = false
                usersDeferred.cancel()
                return@launchWhenCreated
            } catch (e: HttpException) {
                // If response code does not start with digit 2 then something is unusual
                Log.e(TAG, "HttpException, unexpected response")
                binding.progressBar.isVisible = false
                usersDeferred.cancel()
                return@launchWhenCreated
            }

            val userResponse = try {
                usersDeferred.await()
            } catch (e: IOException) {
                // Most likely no internet connection or maybe output stream closed
                Log.e(TAG, "IOException, you might not have internet connection")
                binding.progressBar.isVisible = false
                return@launchWhenCreated
            } catch (e: HttpException) {
                // If response code does not start with digit 2 then something is unusual
                Log.e(TAG, "HttpException, unexpected response")
                binding.progressBar.isVisible = false
                return@launchWhenCreated
            }

            if (checkResponse(todoResponse)) {
                todos = todoResponse.body()!!.toMutableList()
                todoAdapter.todos = todos
            } else {
                Log.e(TAG, "Todo response not successful")
            }

            if (checkResponse(userResponse)) {
                users = userResponse.body()!!.toMutableList()
                users.add(0, User(0, "All Users"))

                dropdownAdapter = ArrayAdapter<User>(this@MainActivity, R.layout.item_user, users)
                binding.dropdownUser.setAdapter(dropdownAdapter)
            } else {
                Log.e(TAG, "User response not successful")
            }

            binding.progressBar.isVisible = false
        }

        setupRecyclerView()

        // Change todos
        binding.dropdownUser.setOnItemClickListener { adapterView, view, i, l ->
            var selectedUserId: Int? = users.get(i).id

            if (selectedUserId == 0) {
                selectedUserId = null
            }

            updateJob?.cancel()
            updateJob=lifecycleScope.launch {
                binding.progressBarSwitchUser.isVisible = true
                binding.rvTodos.isVisible = false

                val newTodos = fetchTodos(selectedUserId)

                // Todo: Effiecient way of updating recyclerview

                if(newTodos != null) {
                    todos.clear()
                    todos.addAll(newTodos)
                    binding.rvTodos.adapter?.notifyDataSetChanged()
                    binding.rvTodos.isVisible = true
                } else {
                    Log.e(TAG, "Could not fetch todos for userid : $selectedUserId")
                }
                binding.progressBarSwitchUser.isVisible = false
            }
        }
    }

    private suspend fun fetchTodos(userId: Int?) : MutableList<Todo>? {
        return withContext(Dispatchers.IO) {
            val todoResponse : Response<List<Todo>>? = try {
                RetrofitInstance.api.getTodos(userId)
            } catch (e: IOException) {
                // Most likely no internet connection or maybe output stream closed
                Log.e(TAG, "IOException, you might not have internet connection")
                null
            } catch (e: HttpException) {
                // If response code does not start with digit 2 then something is unusual
                Log.e(TAG, "HttpException, unexpected response")
                null
            }

            var todos : MutableList<Todo>? = null
            if (checkResponse(todoResponse)) {
                todos = todoResponse!!.body()!!.toMutableList()
            } else {
                Log.e(TAG, "Todo response not successful")
            }

            return@withContext todos
        }
    }

    private fun <T : Any> checkResponse(response: Response<List<T>>?) =
        response?.body() != null && response.isSuccessful

    private fun setupRecyclerView() = binding.rvTodos.apply {
        todoAdapter = TodoAdapter()
        adapter = todoAdapter
        layoutManager = LinearLayoutManager(this@MainActivity)
    }
}