package com.bignerdranch.android.criminalintent


import android.Manifest
import android.Manifest.permission.READ_CONTACTS
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*


private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_CAMERA = 2
private const val REQUEST_PHOTO = 3
private const val DATE_FORMAT = "EEEE, MMM dd, yyyy"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {

    private lateinit var binding: FragmentCrimeBinding
    private lateinit var crime:Crime
    private lateinit var photoFile:File
    private lateinit var photoUri:Uri
    private lateinit var crimeDetailViewModel:CrimeDetailViewModel
    private lateinit var returnIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId:UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel = ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCrimeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer{crime->
                crime?.let{
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(
                            requireActivity(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                            photoFile
                    )
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
        }
        binding.crimeTitle.addTextChangedListener(titleWatcher)

        val detailWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.detail = s.toString()
            }
        }

        binding.crimeDetails.addTextChangedListener(detailWatcher)

        binding.crimeDate.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply{
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        binding.crimeSolved.apply{
            setOnCheckedChangeListener{_,isChecked->
                crime.isSolved = isChecked
            }
        }

        binding.crimeReport.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply{
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

       binding.crimeSuspect.apply{

            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager:PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(pickContactIntent,
                                PackageManager.MATCH_DEFAULT_ONLY)

            if (resolvedActivity == null){
                isEnabled = false
            }
        }

        binding.dialButton.setOnClickListener {
            if (crime.phoneNumber.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Please choose your suspect first",
                    Toast.LENGTH_SHORT
                    ).show()
            } else {
                val phoneUri = Uri.parse("tel: ${crime.phoneNumber}")
                val intent = Intent(Intent.ACTION_DIAL, phoneUri)
                startActivity(intent)
            }
        }

        binding.crimePhoto.setOnClickListener {
            PhotoDisplayDialog.newInstance(photoFile.path, requireContext()).show()
        }


        binding.crimeCamera.apply{
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity:ResolveInfo? = packageManager.resolveActivity(captureImage,
            PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null){
                isEnabled = false
            }

            setOnClickListener{
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities:List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                            PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities){
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(captureImage, REQUEST_PHOTO)
                } else {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
                }

            }
        }
    }

    private fun updateUI(){
        binding.crimeTitle.setText(crime.title)
        binding.crimeDetails.setText(crime.detail)
        binding.crimeDate.text = DateFormat.format(DATE_FORMAT,crime.date).toString()
        binding.crimeSolved.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()){
            binding.crimeSuspect.text = crime.suspect
        }

        updatePhotoView()
    }

    private fun updatePhotoView(){
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            binding.crimePhoto.setImageBitmap(bitmap)
            binding.crimePhoto.contentDescription = getString(R.string.crime_photo_image_description)
        } else{
            binding.crimePhoto.setImageDrawable(null)
            binding.crimePhoto.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    override fun onStop(){
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when{
                resultCode != Activity.RESULT_OK -> return

                requestCode == REQUEST_CONTACT && data != null -> {

                    returnIntent = data
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getCrimeProperty(data)
                    } else {
                        requestPermissions(arrayOf(READ_CONTACTS), REQUEST_CONTACT)
                    }
                }

                requestCode == REQUEST_PHOTO -> {
                    requireActivity().revokeUriPermission(photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    updatePhotoView()
                }
        }
    }

override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CONTACT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCrimeProperty(returnIntent)
                } else {
                    val snackbar = Snackbar.make(
                        binding.root,
                        "We need your permission to select a suspect",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.setAction("Request again"){
                        requestPermissions(arrayOf(READ_CONTACTS), REQUEST_CONTACT)
                    }
                    snackbar.show()
                }
            }
            REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    val snackbar = Snackbar.make(
                        binding.root,
                        "We need your permission to take a picture",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.setAction("Request again"){
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
                    }
                    snackbar.show()
                }
            }
        }
    }

    private fun getCrimeProperty(data: Intent) {
        val contactUri: Uri? = data.data
        val queryFields = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts._ID
        )
        val contentResolver = requireActivity().contentResolver

        val suspectNameCursor = contactUri?.let {
            contentResolver.query(
                it,
                queryFields,
                null,
                null,
                null
            )
        }
        suspectNameCursor?.use {
            if (it.count == 0) {
                return
            }
            it.moveToFirst()
            val nameColumnIndex = suspectNameCursor.getColumnIndex(
                ContactsContract.Contacts.DISPLAY_NAME
            )
            val suspect = it.getString(nameColumnIndex)
            crime.suspect = suspect

            val idColumnIndex = suspectNameCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val id = it.getString(idColumnIndex)
            val phoneQueryField = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val phoneNumberCursor = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                .let { phoneUri ->
                    contentResolver.query(
                        phoneUri,
                        phoneQueryField,
                        "_ID = ?",
                        arrayOf(id),
                        null,
                        null
                    )
                }
            phoneNumberCursor?.use { cursor ->
                cursor.moveToFirst()
                val numberColumnIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                crime.phoneNumber = cursor.getString(numberColumnIndex)
            }
            crimeDetailViewModel.saveCrime(crime)
            binding.crimeSuspect.text = suspect
        }
    }

    private fun getCrimeReport(): String{
        val solvedString = if(crime.isSolved){
            getString(R.string.crime_report_solved)
        } else{
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()

        var suspect = if (crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        } else{
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title,dateString,solvedString,suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID):CrimeFragment{
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }

            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}