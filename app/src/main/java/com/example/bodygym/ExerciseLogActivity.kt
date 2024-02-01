package com.example.bodygym

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


data class Exercise(val name: String, var setList: MutableList<Set> = mutableListOf())
data class Set(
    var setCount: Int,
    var perceivedWeight: Int,
    var repetition: Int,
    var weight: Int,
    var reps: Int,
    var isCompleted: Boolean
)


class ExerciseListAdapter(private val exerciseList: MutableList<Exercise>) :
    RecyclerView.Adapter<ExerciseListAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val exerciseName: TextView = view.findViewById(R.id.text_view_exercise_name)
        val setCount: EditText = view.findViewById(R.id.edit_text_set_count)
        val perceivedWeight: EditText = view.findViewById(R.id.edit_text_perceived_weight)
        val repetition: EditText = view.findViewById(R.id.edit_text_repetition)
        val weight: EditText = view.findViewById(R.id.edit_text_weight)
        val reps: EditText = view.findViewById(R.id.edit_text_reps)
        val completed: CheckBox = view.findViewById(R.id.check_box_completed)
        val buttonAddSet: Button = view.findViewById(R.id.button_add_set) // Add Set 버튼
        val setContainer: ViewGroup = view.findViewById(R.id.set_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exerciseList[position]
        holder.exerciseName.text = exercise.name

        // 기존에 추가되었던 세트 뷰들을 모두 제거
        holder.setContainer.removeAllViews()

        // 마지막으로 추가된 세트 정보
        val lastSet = exercise.setList.lastOrNull()

        // EditText에 마지막으로 추가된 세트 정보를 설정
        holder.setCount.setText(lastSet?.setCount?.toString() ?: "")
        holder.perceivedWeight.setText(lastSet?.perceivedWeight?.toString() ?: "")
        holder.repetition.setText(lastSet?.repetition?.toString() ?: "")
        holder.weight.setText(lastSet?.weight?.toString() ?: "")
        holder.reps.setText(lastSet?.reps?.toString() ?: "")
        holder.completed.isChecked = lastSet?.isCompleted ?: false

        // 모든 세트의 정보를 뷰에 바인딩
        for (set in exercise.setList) {
            // 세트 정보를 보여주는 뷰 생성 (예: TextView)
            val setView = TextView(holder.itemView.context)
            setView.text =
                "${set.setCount}, ${set.perceivedWeight}, ${set.repetition}, ${set.weight},${set.reps}"

            // 세트 뷰를 컨테이너에 추가
            holder.setContainer.addView(setView)
        }

        // Add Set 버튼 클릭 리스너
        holder.buttonAddSet.setOnClickListener {
            // 사용자가 입력한 세트 정보를 가져옴
            // 입력이 없으면 마지막으로 추가된 세트의 정보를 사용
            val setCount = holder.setCount.text.toString().toIntOrNull() ?: lastSet?.setCount ?: 0
            val perceivedWeight =
                holder.perceivedWeight.text.toString().toIntOrNull() ?: lastSet?.perceivedWeight
                ?: 0
            val repetition =
                holder.repetition.text.toString().toIntOrNull() ?: lastSet?.repetition ?: 0
            val weight = holder.weight.text.toString().toIntOrNull() ?: lastSet?.weight ?: 0
            val reps = holder.reps.text.toString().toIntOrNull() ?: lastSet?.reps ?: 0
            val isCompleted = holder.completed.isChecked

            // 현재 운동의 세트 리스트에 새로운 세트 추가
            exercise.setList.add(
                Set(
                    setCount = setCount,
                    perceivedWeight = perceivedWeight,
                    repetition = repetition,
                    weight = weight,
                    reps = reps,
                    isCompleted = isCompleted
                )
            )
            notifyItemChanged(position) // 해당 아이템에 데이터 변경 알림
        }
    }

    override fun getItemCount() = exerciseList.size
}

class ExerciseLogActivity : AppCompatActivity() {

    private lateinit var restTimeSpinner: Spinner
    private lateinit var restTimeAdapter: ArrayAdapter<String>
    private lateinit var textViewExerciseTime: TextView
    private lateinit var textViewRestTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_log)

        val exerciseMap = hashMapOf(
            "하체" to arrayOf("바벨백스쿼트", "컨벤셔널데드리프트"),
            "가슴" to arrayOf("벤치프레스", "인클라인벤치프레스"),
            "등" to arrayOf("풀업", "케이블 풀다운"),
            "어깨" to arrayOf("오버헤드프레스"),
            "팔" to arrayOf("바벨컬")
        )

        lateinit var restTimeSpinner: Spinner
        restTimeSpinner = findViewById(R.id.spinner_rest_time)
        val exerciseList = mutableListOf<Exercise>() // 운동 항목을 저장할 리스트
        val exerciseAdapter = ExerciseListAdapter(exerciseList) // 어댑터 생성

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_exercises)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = exerciseAdapter // RecyclerView에 어댑터 설정


        val buttonAddExercise: Button = findViewById(R.id.button_add_exercise)
        buttonAddExercise.setOnClickListener {
            Log.d("ExerciseLogActivity", "buttonAddExercise 클릭")  // 버튼 클릭 로그
            val builder = AlertDialog.Builder(this@ExerciseLogActivity)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.exercise_selection_dialog, null)
            builder.setView(dialogView)

            val categorySpinner: Spinner = dialogView.findViewById(R.id.spinner_exercise_category)
            val exercisesSpinner: Spinner = dialogView.findViewById(R.id.spinner_exercises)

            // Spinner에 데이터를 설정하는 코드
            val categoryAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                exerciseMap.keys.toTypedArray()
            )
            categorySpinner.adapter = categoryAdapter

            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    pos: Int,
                    id: Long
                ) {
                    val selectedCategory = parent.getItemAtPosition(pos).toString()
                    val exercises = exerciseMap[selectedCategory]
                    val exerciseAdapter = ArrayAdapter(
                        this@ExerciseLogActivity,
                        android.R.layout.simple_spinner_item,
                        exercises!!
                    )
                    exercisesSpinner.adapter = exerciseAdapter
                    Log.d(
                        "ExerciseLogActivity",
                        "Spinner 항목 선택: $selectedCategory"
                    )  // Spinner 항목 선택 로그
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }

            builder.setPositiveButton("추가") { dialog, _ ->
                val selectedExercise = exercisesSpinner.selectedItem.toString() // 선택한 운동 항목
                exerciseList.add(Exercise(selectedExercise)) // 리스트에 추가
                exerciseAdapter.notifyDataSetChanged() // 어댑터에 데이터 변경 알림
                dialog.dismiss()
            }

            builder.setNegativeButton("취소") { dialog, _ ->
                Log.d("ExerciseLogActivity", "취소 버튼 클릭")  // 취소 버튼 클릭 로그
                dialog.dismiss()
            }

            builder.show()
        }

        textViewExerciseTime = findViewById(R.id.text_view_exercise_time)
        textViewRestTime = findViewById(R.id.text_view_rest_time)

        var exerciseTime = 0L
        var restTime = 0L

        // 쉬는 시간 선택을 위한 Spinner 설정
        val restTimeOptions = arrayOf(
            "0초",
            "30초",
            "1분",
            "1분 30초",
            "2분",
            "2분 30초",
            "3분",
            "3분 30초",
            "4분",
            "4분 30초",
            "5분"
        )

        textViewExerciseTime.text = "00:00"
        textViewRestTime.text = "00:00"

        val buttonStartExercise: Button = findViewById(R.id.button_start_exercise)
        val buttonEnd: Button = findViewById(R.id.button_end)
        buttonStartExercise.setOnClickListener {
            // `restTimer`를 시작하기 전에 `textViewRestTime`에 초기 값을 설정
            val initialRestMinutes = restTime / 1000 / 60
            val initialRestSeconds = restTime / 1000 % 60
            textViewRestTime.text = String.format("%02d:%02d", initialRestMinutes, initialRestSeconds)

            // `restTimer`를 새로 생성하고 시작
            val restTimer = object : CountDownTimer(restTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    restTime -= 1000
                    val minutes = restTime / 1000 / 60
                    val seconds = restTime / 1000 % 60
                    textViewRestTime.text = String.format("%02d:%02d", minutes, seconds)
                }

                override fun onFinish() {
                    // Rest time finished
                }
            }.start()

            // `exerciseTimer`를 시작하기 전에 `textViewExerciseTime`에 초기 값을 설정
            exerciseTime = 0L
            val initialExerciseMinutes = exerciseTime / 1000 / 60
            val initialExerciseSeconds = exerciseTime / 1000 % 60
            textViewExerciseTime.text = String.format("%02d:%02d", initialExerciseMinutes, initialExerciseSeconds)

            // `exerciseTimer`를 새로 생성하고 시작
            val exerciseTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    exerciseTime += 1000
                    val minutes = exerciseTime / 1000 / 60
                    val seconds = exerciseTime / 1000 % 60
                    textViewExerciseTime.text = String.format("%02d:%02d", minutes, seconds)
                }

                override fun onFinish() {
                    // Exercise time finished
                }
            }.start()

            buttonStartExercise.visibility = View.GONE
            buttonEnd.visibility = View.VISIBLE
        }

        restTimeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                val selectedRestTime = parent.getItemAtPosition(pos).toString()
                restTime = when (selectedRestTime) {
                    "30초" -> 30000L
                    "1분" -> 60000L
                    "1분 30초" -> 90000L
                    "2분" -> 120000L
                    "2분 30초" -> 150000L
                    "3분" -> 180000L
                    "3분 30초" -> 210000L
                    "4분" -> 240000L
                    "4분 30초" -> 270000L
                    "5분" -> 300000L
                    else -> 0L
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        val restTimer = object : CountDownTimer(restTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                textViewRestTime.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                // Rest time finished
            }
        }.start()
    }
}