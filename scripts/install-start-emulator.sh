# if there's already an emulator, just exit
#export PATH=${ANDROID_HOME}:${ANDROID_HOME}/emulator:${TOOLS}:${TOOLS}/bin:${ANDROID_HOME}/platform-tools:${PATH}
#echo "users file dump:"
#cat /etc/group
#sudo adduser $USER libvirt
#sudo adduser $USER kvm
#echo "users file dump:"
#cat /etc/group
#echo "current user:"
#echo $USER
#echo "current user (whoami):"
#echo whoami

# If there is an emulator already started, just use that one

echo "run: started=$(adb get-state 1)"
started=$(adb get-state 1)
if [ "$started" = "device" ]; then
  exit 0
fi

# Set PATH variable so we can find sdkmanager and avdmanager
echo "Setting Path (emulator before tools hack)"
export ANDROID_SDK=$HOME/Library/Android/sdk
export PATH=$ANDROID_SDK/emulator:$ANDROID_SDK/tools/:$ANDROID_HOME/tools/bin:$PATH
#echo "run: sdkmanager tools"
#sdkmanager tools
#echo "run: kill-server"
#adb kill-server
#echo "closing emulator"
#echo "run: adb devices"
#adb devices
#echo "run: adb emu kill &"
#adb emu kill &
#echo "run: adb devices | grep emulator | cut -f1 | while read line; do adb -s $line emu kill; done &"
#adb devices | grep emulator | cut -f1 | while read line; do adb -s $line emu kill; done &
echo "starting emulator"
echo "run: echo yes | sdkmanager --install \"system-images;android-28;default;x86\""
echo yes | sdkmanager --install "system-images;android-29;default;x86"

# create new Emulator
echo "run: echo no | avdmanager create avd --force -n test -k \"system-images;android-29;default;x86\" -c 10M"
echo no | avdmanager create avd --force -n test -k "system-images;android-29;default;x86" -c 10M

# sanity check, make sure new Emulator was created
echo "run: emulator -list-avds"
emulator -list-avds

# start Emulator (not sure if we actually have to run this as sudo or not
echo "run: sudo -E sudo -u $USER -E bash -c \"./emulator -avd test -verbose -no-snapshot -no-window -camera-back none -camera-front none -selinux permissive -qemu -m 2048 &\""
sudo -E sudo -u "$USER" -E bash -c "${ANDROID_HOME}/emulator/emulator -avd test -verbose -no-snapshot -no-window -camera-back none -camera-front none -selinux permissive -qemu -m 2048 &"

echo "run: adb shell input keyevent 82 &"
adb shell input keyevent 82 &

#block until emulator is ready to run tests
echo "Waiting for Emulator to start"
until [[ "$bootanim" =~ "stopped" ]]; do
  echo "run: adb reconnect offline"
  adb reconnect offline
  adb devices
  bootanim=`adb -e shell getprop init.svc.bootanim 2>&1 &`
  echo "Waiting for emulator, status is: $bootanim"
  sleep 1
  echo
done

echo "Emulator has started - dismissing keyboard and sleeping for 1 second."
adb shell wm dismiss-keyguard
sleep 1
