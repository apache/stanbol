@echo off

ant clean

ant

java -Xmx1g -cp="bin;lib/sphinx4.jar;lib/log4j-1.2.15.jar;lib/mary-common.jar;lib/js.jar;resource/am/WSJ_8au_13dCep_16k_40mel_130Hz_6800Hz.jar" org.apache.stanbol.enhancer.interaction.Start
