package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeListBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import java.util.*

class CrimeListFragment : Fragment() {

    interface Callbacks{
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks:Callbacks?=null
    private lateinit var binding: FragmentCrimeListBinding
    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())

    private lateinit var crimeListViewModel:CrimeListViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.crimeRecyclerView.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel = ViewModelProvider(this).get(CrimeListViewModel::class.java)
        crimeListViewModel.crimeListLiveData.observe(viewLifecycleOwner, Observer{crimes->
            crimes?.let{
                updateUI(crimes)
            }
        })
    }

    private fun updateUI(crimes:List<Crime>){
        adapter = CrimeAdapter(crimes)
        binding.crimeRecyclerView.adapter = adapter
    }

    private inner class CrimeAdapter(var crimes:List<Crime>): RecyclerView.Adapter<CrimeHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val binding = ListItemCrimeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CrimeHolder(binding.root, binding)
        }

        override fun getItemCount() = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }
    }

    private inner class CrimeHolder(view:View, val binding: ListItemCrimeBinding):RecyclerView.ViewHolder(view), View.OnClickListener{

        private lateinit var crime: Crime

        init{
            view.setOnClickListener(this)
        }

        fun bind(crime:Crime){
            this.crime = crime
            binding.crimeTitle.text = crime.title
            binding.crimeDate.text = DateFormat.format("EEEE, MMM dd, yyyy", crime.date)
            binding.crimeSolved.visibility = if(crime.isSolved){
                View.VISIBLE
            } else{
                View.GONE
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks =  null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.new_crime ->{
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else-> return super.onOptionsItemSelected(item)
        }
    }
}