package com.example.fitsutra

import HomeFragment
import ReportFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fitsutra.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())


        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.home -> replaceFragment(HomeFragment())
                R.id.tracker -> replaceFragment(TrackerFragment())
                R.id.exercise -> replaceFragment(ExerciseFragment())
                R.id.setting -> replaceFragment(SettingFragment())
                R.id.report -> replaceFragment(ReportFragment())
                else ->{

                }

            }
            true


        }
    }

    private fun replaceFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransition = fragmentManager.beginTransaction()
        fragmentTransition.replace(R.id.frame_layout,fragment)
        fragmentTransition.commit()
    }
}
