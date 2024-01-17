package com.example.compose_architecture

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.compose_architecture.ui.theme.Compose_ArchitectureTheme

class ComposeLiveData : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Compose_ArchitectureTheme {
                Surface(Modifier.fillMaxSize()) {
                    TopLevel()
                }
            }
        }
    }
}

class ToDoLiveData : ViewModel(){
//    val text = mutableStateOf("")
    private val _text = MutableLiveData("")
    val text : LiveData<String> = _text
    fun setText(text:String){
        _text.value=text
    }
    val setText2 : (String) ->Unit ={
        _text.value=it
    }

    //mutableStateListOf를 LiveData로 바꿔서 observerAsState로 하는 방식이 좋지 않음
    //livedata를 가지고 state를 만들 때는 리스트 자체가 바뀌지 업데이트 되지 않으면(항목 업데이트되도 안됨) 업데이트가 되지 않는다.

  //  val toDoList = mutableStateListOf<ToDoData>()
//mutableStateListOf같은 경우는 추가,삭제,대입 같은 경우 ui가 갱신이 된다. 각 항목의 필드가 바뀌었을 때는 갱신이 안되는 문제 발생
    //LiveData<Llist<ToDoData>>.observerAsState - List가 통채로 다른 리스트로 바뀌었을 때만 State가 갱신된

    private val _rawtoDoList = mutableListOf<ToDoData>()
    private val _toDoList = MutableLiveData<List<ToDoData>>(_rawtoDoList)
    val toDoList : LiveData<List<ToDoData>> = _toDoList

    val onSubmit: (String) -> Unit = {
        val key = (_rawtoDoList.lastOrNull()?.key ?: 0) + 1
        _rawtoDoList.add(ToDoData(key, it))
        _toDoList.value=mutableListOf<ToDoData>().also {
            it.addAll(_rawtoDoList)
        }
        _text.value=""
    }
    val onToggle: (Int, Boolean) -> Unit = { key, checked ->
        val i = _rawtoDoList.indexOfFirst { it.key == key }
        _rawtoDoList[i] = _rawtoDoList[i].copy(done = checked)
        _toDoList.value=mutableListOf<ToDoData>().also {
            it.addAll(_rawtoDoList)
        }
    }
    val onDelete: (Int) -> Unit = { key ->
        val i = _rawtoDoList.indexOfFirst { it.key == key }
        _rawtoDoList.removeAt(i)
        _toDoList.value=mutableListOf<ToDoData>().also {
            it.addAll(_rawtoDoList)
        }
    }
    val onEditing: (Int, String) -> Unit = { key, text ->
        val i = _rawtoDoList.indexOfFirst { it.key == key }
        _rawtoDoList[i] = _rawtoDoList[i].copy(text = text)
        _toDoList.value=mutableListOf<ToDoData>().also {
            it.addAll(_rawtoDoList)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopLevel(viewModel:ToDoLiveData= viewModel()){


    Scaffold {
        Column {
            ToDoInput(
                text = viewModel.text.observeAsState("").value,
                onTexetChanged = {
                                 viewModel.setText(it)
                },
                onSubmit = viewModel.onSubmit,
            )
            val items = viewModel.toDoList.observeAsState(emptyList()).value
            LazyColumn {
                //key를 설정해서 조금 더 효율적으로 렌더링 가능
                items(items,key = {it.key}) {
                    ToDo(
                        todoData = it,
                        onToggle = viewModel.onToggle,
                        onDelete = viewModel.onDelete,
                        onEdit = viewModel .onEditing
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToDoInput(
    text: String,
    onTexetChanged: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text, onValueChange = onTexetChanged,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(onClick = { onSubmit(text) }) {
            Text(text = "입력")
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToDo(
    todoData: ToDoData,
    onEdit: (Key: Int, text: String) -> Unit = { _, _ -> },
    onToggle: (Key: Int, checkd: Boolean) -> Unit = { _, _ -> },
    onDelete: (Key: Int) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp)
    ) {

        Crossfade(targetState = isEditing, label = "") {
            when (it) {
                false -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = todoData.text, Modifier.weight(1f))
                        Text(text = "완료")
                        Checkbox(checked = todoData.done, onCheckedChange = {
                            onToggle(todoData.key, it)
                        })
                        Button(onClick = {
                            isEditing = true
                        }) {
                            Text(text = "수정")
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        Button(onClick = { onDelete(todoData.key) }) {
                            Text(text = "삭제")
                        }
                    }
                }

                true -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        var newText by remember { mutableStateOf(todoData.text) }
                        OutlinedTextField(
                            value = newText, onValueChange = {newText=it},
                            Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Button(onClick = {
                            onEdit(todoData.key,newText)
                            isEditing = false
                        }) {
                            Text(text = "완료")
                        }

                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DefaultInputPreview() {
    Compose_ArchitectureTheme {
        ToDoInput(text = "test", onTexetChanged = {}, onSubmit = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ToDoPreview() {
    Compose_ArchitectureTheme {
        ToDo(ToDoData(1, "test", false))
    }
}



