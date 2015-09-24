#pragma rtGlobals=3		// Use modern global access method and strict wave access.


Menu "Macros"
	"Load and plot LIV", DoOpenFileDialog(1)
	"Load and plot spectra", DoOpenFileDialog(2)
	"Load and plot intermode beatnote", DoOpenFileDialog(3)
	//"Load and plot temperature", DoOpenFileDialog(4)
End
End

////////////////////////////////////

Structure RGB 

	Variable r
	Variable g
	Variable b

EndStructure

////////////////////////////////////

Structure fileProperties
	String fileName
  	String temperature
  	String powerResitor // for temperature measurement
  	String dc // duty cycle
  	String RBW // resolution bandwidth
  	String span
  	String current
  	Variable centerFreq // for shifting intermode beatnote
  	Variable yAxisAreaStart, yAxisAreaEnd // for spectra and intermode beatnote to plot several graphs
  																// within same window
  	Variable fileNumber
  	// rgb code for the color of the graph 
  	Variable r
  	Variable g
  	Variable b 
EndStructure

// Filename guideline
// To allow the function DoOpenFileDialog to extract relevant information
// from the names of the files it is necessary to name the files properly:
// ("*" denotes arbitrary sequence of characters)
// - to extract temperature: "*_tempdeg*" where "temp" is to be substituted
// with corresponding temperature
// - to extract current: "*_currentmA*" where "current" is to be substituted with the corresponding value
// 
// Improper naming can cause problems!

///////////////////////////////////

Function/S DoOpenFileDialog(measurement)
	
   Variable measurement // 1 - LIV, 2 - Spectra, 3 - Intermode beatnote
   Variable refNum
   String message = "Select one or more files"
   String outputPaths
   String promptMsg
   String fileFilters = "All files" 
   String windowName
   Variable fileCounter = 0
   Variable colorRow = 0 // for generating color (c.f. generateColor() function)
	
	ColorTab2Wave Rainbow
	WAVE rainbow = M_colors
	
	Open /D /R /MULT=1 /F=fileFilters /M=message refNum 
	outputPaths = S_fileName
	
	if (strlen(outputPaths) == 0)
		Print "Cancelled"
	else
		Variable numFilesSelected = ItemsInList(outputPaths, "\r")
		Variable tempIndex
		Variable i, j
		Variable plotRatio = 1/numFilesSelected
		String temperature
		
		promptMsg = "Enter name of the graph"
		Prompt windowName, promptMsg
		DoPrompt promptMsg, windowName	
		
		for (i=0; i<numFilesSelected; i+=1)
		
			fileCounter += 1
			
			STRUCT fileProperties file
			String currentFile = StringFromList(i,outputPaths, "\r")
			file.fileName = currentFile
			generateColor(file,colorRow,rainbow)
			colorRow += 18
			temperature = extractTemperature(currentFile)		
			
			if (stringmatch(temperature, "NA"))
				temperature = file.fileName // thus complete path to the file will show up in the input field
				promptMsg = "Enter parameters"
				Prompt temperature, "Enter temperature for "
				DoPrompt promptMsg, temperature
			endif
			
			file.fileNumber = fileCounter
			file.temperature = temperature	
			switch (measurement)	
				case 1: 
					file.dc = extractDC(file.filename)
					LoadAndPlotLIV(file, windowName)
					break
				case 2:
					String current = extractCurrent(file.fileName)
					if (stringmatch(current, "NA"))
						current = file.fileName // thus complete path to the file will show up in the input field
						promptMsg = "Enter parameters"
						Prompt current, "Enter current for "
						DoPrompt promptMsg, current
					endif 
					file.yAxisAreaStart = plotRatio * i
					file.yAxisAreaEnd = plotRatio * (i+1)
					file.current = current
					LoadAndPlotSpectra(file, windowName)
					break
				case 3:
					current = extractCurrent(file.fileName)
					if (stringmatch(current, "NA"))
						current = file.fileName // thus complete path to the file will show up in the input field
						promptMsg = "Enter parameters"
						Prompt current, "Enter current for "
						DoPrompt promptMsg, current
					endif
					file.yAxisAreaStart = plotRatio * i
					file.yAxisAreaEnd = plotRatio * (i+1)
					file.current = current
					LoadAndPlotIntermodeBeat(file, WindowName)
					break
				case 4:
					break
					
			endswitch
		
		endfor
	endif
	

	return outputPaths // Will be empty if user canceled

End

////////////////////////////////////

Function LoadAndPlotLIV(file, windowName)
	
	
	STRUCT fileProperties &file
	String windowName
	
	LoadWave /A/J/D/W/O/N/E=1/K=0/V={" ","$",0,0} file.fileName
	if (V_flag == 0)
		Print "Could not load " + file.filename
		return -1
	endif
	
	String temperature = file.temperature
	
	//Prompt temperature, "Enter temperature: "
	//DoPrompt "Parameters", temperature
	
	String current, voltage, power, avgPower
	current = "Current_mA_" + temperature + "C" + "_" + file.dc
	voltage = "Voltage_V_" + temperature + "C" + "_" + file.dc
	power = "Power_mW_" + temperature + "C" + "_" + file.dc
	avgPower = "Avg_power_mW_" + temperature + "C" + "_" + file.dc
	Variable ifCW = 0;	
      
	
	Rename wave0, $current
	Rename wave1, $voltage
	Rename wave2, $power
	if (cmpstr(file.dc, "CW") != 0)
		Rename wave3, $avgPower
		WAVE avgPowerWave = $avgPower
	else
		ifCW = 1;
	endif
	WAVE currentWave = $current
	WAVE voltageWave = $voltage
	WAVE powerWave = $power
	currentWave *= 1000 // convert to milliamps (comment out if not needed)
	
	String AvgPowerWindowName = windowName + "avgPower" 
		
		
	///////////////// peak power ///////////////////////////	
	if (WinType(windowName) == 0) // if LIV graph doesn't exist yet create a new one
   	
   	Display/N=$windowName
   	AppendToGraph/W=$windowName $voltage vs $current 
   	AppendToGraph/W=$windowName/R $power vs $current
   	// setting axes labels
   	Label /W=$windowName /Z bottom, "Current [mA]"
   	Label /W=$windowName /Z left, "Voltage [V]"
   	Label /W=$windowName /Z right, "Peak Power [mW]"
   	// setting minor ticks
   	ModifyGraph /W=$windowName minor(bottom)=1
   	ModifyGraph /W=$windowName minor(left)=1
   	ModifyGraph /W=$windowName minor(right)=1
   	// setting traces' colors
   	ModifyGraph /W=$windowName rgb($voltage) = (file.r, file.g, file.b)
   	ModifyGraph /W=$windowName rgb($power) = (file.r, file.g, file.b)
   	endif
   	
   if (WinType(AvgPowerWindowName) == 0)
   	///////////////// average power/////////////////////////
   	if (ifCW == 0)
   	Display/N=$AvgPowerWindowName
   	AppendToGraph/W=$AvgPowerWindowName $voltage vs $current 
   	AppendToGraph/W=$AvgPowerWindowName/R $avgPower vs $current
   	// setting axes labels
   	Label /W=$AvgPowerWindowName /Z bottom, "Current [mA]"
   	Label /W=$AvgPowerWindowName /Z left, "Voltage [V]"
   	Label /W=$AvgPowerWindowName /Z right, "Average Power [mW]"
   	// setting minor ticks
   	ModifyGraph /W=$AvgPowerWindowName minor(bottom)=1
   	ModifyGraph /W=$AvgPowerWindowName minor(left)=1
   	ModifyGraph /W=$AvgPowerWindowName minor(right)=1
   	endif
   	if (ifCW == 1)
   	Display/N=$AvgPowerWindowName
   	AppendToGraph/W=$AvgPowerWindowName $voltage vs $current 
   	AppendToGraph/W=$AvgPowerWindowName/R $power vs $current
   	// setting axes labels
   	Label /W=$AvgPowerWindowName /Z bottom, "Current [mA]"
   	Label /W=$AvgPowerWindowName /Z left, "Voltage [V]"
   	Label /W=$AvgPowerWindowName /Z right, "Average Power [mW]"
   	// setting minor ticks
   	ModifyGraph /W=$AvgPowerWindowName minor(bottom)=1
   	ModifyGraph /W=$AvgPowerWindowName minor(left)=1
   	ModifyGraph /W=$AvgPowerWindowName minor(right)=1
   	endif
   	ModifyGraph /W=$AvgPowerWindowName rgb($voltage) = (file.r, file.g, file.b)
   	ModifyGraph /W=$AvgPowerWindowName rgb($power) = (file.r, file.g, file.b)
   	return NaN
   
   endif
	AppendToGraph/W=$windowName $voltage vs $current 
   	AppendToGraph/W=$windowName/R $power vs $current
   	AppendToGraph/W=$AvgPowerWindowName $voltage vs $current 
   	ModifyGraph /W=$windowName rgb($voltage) = (file.r, file.g, file.b)
   	ModifyGraph /W=$windowName rgb($power) = (file.r, file.g, file.b)
	if (ifCW == 0)
	AppendToGraph/W=$AvgPowerWindowName/R $avgPower vs $current
	ModifyGraph /W=$AvgPowerWindowName rgb($voltage) = (file.r, file.g, file.b)
   	ModifyGraph /W=$AvgPowerWindowName rgb($avgPower) = (file.r, file.g, file.b)
	else
	AppendToGraph/W=$AvgPowerWindowName/R $power vs $current
	ModifyGraph /W=$AvgPowerWindowName rgb($voltage) = (file.r, file.g, file.b)
   	ModifyGraph /W=$AvgPowerWindowName rgb($power) = (file.r, file.g, file.b)
	endif
	 
End

////////////////////////////////////////////////////////

Function LoadAndPlotSpectra(file, windowName)
	
	STRUCT fileProperties &file
	String windowName
	Variable waveMinimum
	
	LoadWave /A/J/D/W/O/N/E=1/K=0/V={"\t,","$",0,0} file.fileName 
	
	if (V_flag == 0)
		Print "Could not load " + file.filename
		return -1
	endif
	
	String wavenumber, intensity
	
	wavenumber = "Wn_inv_cm_" + file.current + "_" + file.temperature 
	intensity = "I_" + file.current + "_" + file.temperature 
	Rename wave0, $wavenumber
	Rename wave1, $intensity
	WAVE wavenumberWave = $wavenumber
	WAVE intensityWave = $intensity 
	
	String windowNameLog = windowName + "_log"
	
	if (Wintype(windowName) == 0 && Wintype(windowNameLog) == 0)
	
		Display/N=$windowName
		AppendToGraph/W=$windowName $intensity vs $wavenumber
		
		Label /W=$windowName /Z bottom, "Wavenumber [cm\\S-1\\M]"
		
		ModifyGraph /W=$windowName minor(bottom)=1
   		ModifyGraph /W=$windowName minor(left)=1
   		ModifyGraph /W=$windowName axisEnab(left) = {file.yAxisAreaStart, file.yAxisAreaEnd}
   		
   		Display/N=$windowNameLog
		AppendToGraph/W=$windowNameLog $intensity vs $wavenumber
		
		Label /W=$windowNameLog /Z bottom, "Wavenumber [cm\\S-1\\M]"
		
		ModifyGraph /W=$windowNameLog minor(bottom)=1
   		ModifyGraph /W=$windowNameLog minor(left)=1
   		ModifyGraph /W=$windowNameLog axisEnab(left) = {file.yAxisAreaStart, file.yAxisAreaEnd}
   		ModifyGraph /W=$windowNameLog log(left)=1
   		
   		waveMinimum = WaveMin(IntensityWave)
   		
   		//IntensityWave += waveMinimum
   	
   	return NaN
	
	endif
	
	String leftAxisName = "L"+num2str(file.fileNumber)
	AppendToGraph/W=$windowName/L=$leftAxisName $intensity vs $wavenumber
	ModifyGraph /W=$windowName axisEnab($leftAxisName) = {file.yAxisAreaStart, file.yAxisAreaEnd}
	ModifyGraph /W=$windowName freePos($leftAxisName) = 0
	
	AppendToGraph/W=$windowNameLog/L=$leftAxisName $intensity vs $wavenumber
	ModifyGraph /W= $windowNameLog axisEnab($leftAxisName) = {file.yAxisAreaStart, file.yAxisAreaEnd}
	ModifyGraph/W=$windowNameLog freePos($leftAxisName) = 0
	ModifyGraph /W=$windowNameLog log($leftAxisName)=1
	
	waveMinimum = WaveMin(IntensityWave)
   		
   	//IntensityWave += waveMinimum
   	
	
End

////////////////////////////////////////////////////////

Function LoadAndPlotIntermodeBeat(file, WindowName)
	STRUCT fileProperties &file	
	String WindowName
	LoadWave /T/A/J/D/W/O/N/E=1/K=0/V={";","$",0,0}/L={0,28,0,0,0} file.fileName
	
	if (V_flag == 0)
		Print "Could not load " + file.filename
		return -1
	endif
	
	String frequency, powerdBm
	frequency = "Frequency_Hz_" + file.current + "_" + file.temperature
	powerdBm = "Power_dBm_" + file.current + "_" + file.temperature 
	Rename Type, $frequency
	Rename FSU, $powerdBm
	WAVE frequencyWave = $frequency
	WAVE powerdBmWave = $powerdBm
	
	if (Wintype(WindowName) == 0)
		Display/N=$WindowName
		
		FindValue /V = (WaveMax(powerdBmWave)) powerdBmWave
   
   	file.centerFreq = frequencyWave[V_value]
		
	AppendToGraph/W=$WindowName powerdBmWave vs frequencyWave
		
	Label /W=$WindowName /Z bottom, "Frequency [Hz]"
	ModifyGraph /W=$WindowName minor(bottom)=1
   	ModifyGraph /W=$WindowName minor(left)=1
   	ModifyGraph /W=$WindowName axisEnab(left) = {file.yAxisAreaStart, file.yAxisAreaEnd}
   	
   	frequencyWave -= file.centerFreq // comment out to kick out centering
   	
   	return NaN
   
   endif
   Variable maxi = WaveMax(powerdBmWave)
   
   FindValue /V = (WaveMax(powerdBmWave)) powerdBmWave
   
   file.centerFreq = frequencyWave[V_value]
   
	String leftAxisName = "L"+num2str(file.fileNumber)
	AppendToGraph/W=$WindowName/L=$leftAxisName powerdBmWave vs frequencyWave
	ModifyGraph /W=$WindowName axisEnab($leftAxisName) = {file.yAxisAreaStart, file.yAxisAreaEnd}
	ModifyGraph /W=$WindowName freePos($leftAxisName) = 0
	frequencyWave -= file.centerFreq
End

////////////////////////////////////////////////////////

Function/S extractTemperature(filename)
	String filename
	String temperature
	Variable tempIndex
		
	tempIndex = strsearch(filename,"deg",0)
	
	if (tempIndex == -1)
		return "NA"
	endif
	
	Variable j
	for (j=tempIndex; j>0; j-=1)
		if (stringmatch(filename[j],"_"))
			temperature = filename[j+1,tempIndex-1]
			break
		endif
	endfor
	
	return temperature

End

////////////////////////////////////////////////////////

// for spectra
Function/S extractCurrent(filename)
	
	String filename
	String current
	Variable curIndex

	curIndex = strsearch(filename, "mA", 0)
	if (curIndex == -1)
		curIndex = strsearch(filename, "MA", 0)
		if (curIndex == -1)
			return "NA"
		endif
	endif
	
	Variable i
	
	for (i = curIndex; i > 0; i-=1)
		if (stringmatch(filename[i], "_"))
			current = filename[i+1,curIndex+1]
			break
		endif
	endfor
	
	return current

End

////////////////////////////////////////////////////////
Function/S extractDC(filename)
	
	String filename
	String dc
	Variable dcIndex
	
	dcIndex = strsearch(filename, "dc", 0)
	if (dcIndex == -1)
		return "CW"
	endif
	
	Variable i
	
	for (i = dcIndex; i>0; i-=1)
		if (stringmatch(filename[i], "_"))
			dc = filename[i+1, dcIndex+1]
			break
		endif
	endfor
	
	return dc
	
End

// Color generator for traces

Function generateColor(file, row, rainbow)

	STRUCT fileProperties &file
	//Variable r, g, b
	Variable row 
	WAVE rainbow
	
	file.r = rainbow[row][0]
	file.g = rainbow[row][1]
	file.b = rainbow[row][2]
	
End

