import {
  DeviceEventEmitter,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

const NativeRNTritonPlayer = NativeModules.RNTritonPlayer;

class RNTritonPlayer {
  static configure({brand}) {
    NativeRNTritonPlayer.configure(brand);
  }

  static play(tritonName, tritonMount) {
    NativeRNTritonPlayer.play(tritonName, tritonMount);
  }

  static getCurrentPlaybackTime(successCallback, errorCallback) {
    if (Platform.OS === 'ios') {
      NativeRNTritonPlayer.getCurrentPlaybackTime(
        successValue => {
          //ios returns this value in seconds - Android value comes back in milliseconds
          successCallback(successValue);
        },
        errorValue => {
          errorCallback(errorValue);
        },
      );
    } else if (Platform.OS == 'android') {
      NativeRNTritonPlayer.getCurrentPlaybackTime(
        successValue => {
          //Android value comes back in milliseconds - ios returns this value in seconds
          successCallback(Math.floor(successValue / 1000));
        },
        errorValue => {
          errorCallback(errorValue);
        },
      );
    } else {
      console.log(
        `getCurrentPlaybackTime is not currently supported on your platform '${Platform.OS}'`,
      );
    }
  }

  /** Seek by `offset` milliseconds [negative to seek back] */
  static seek(offset) {
    if (Platform.OS === 'ios') {
      NativeRNTritonPlayer.getCurrentPlaybackTime(
        successValue => {
          let newOffset = offset + successValue * 1000;
          if (newOffset < 0) {
            newOffset = 0;
          }

          NativeRNTritonPlayer.seekTo(newOffset);
        },
        errorValue => {
          console.log('error', errorValue);
        },
      );
    } else {
      console.log(
        `seek is not currently supported on your platform '${Platform.OS}'`,
      );
    }
  }

  static seekTo(offset) {
    if (Platform.OS === 'ios') {
      NativeRNTritonPlayer.seekTo(offset);
    } else {
      console.log(
        `seekTo is not currently supported on your platform '${Platform.OS}'`,
      );
    }
  }

  static pause() {
    NativeRNTritonPlayer.pause();
  }

  static unPause() {
    NativeRNTritonPlayer.unPause();
  }

  static playOnDemandStream(trackURL) {
    NativeRNTritonPlayer.playOnDemandStream(trackURL);
  }

  static stop() {
    NativeRNTritonPlayer.stop();
  }

  static quit() {
    NativeRNTritonPlayer.quit();
  }

  static addStreamChangeListener(callback) {
    if (Platform.OS === 'ios') {
      const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
      tritonEmitter.addListener('streamChanged', callback);
    } else {
      DeviceEventEmitter.addListener('streamChanged', callback);
    }
  }

  static addTrackChangeListener(callback) {
    if (Platform.OS === 'ios') {
      const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
      tritonEmitter.addListener('trackChanged', callback);
    } else {
      DeviceEventEmitter.addListener('trackChanged', callback);
    }
  }

  static addStateChangeListener(callback) {
    if (Platform.OS === 'ios') {
      const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
      tritonEmitter.addListener('stateChanged', callback);
    } else {
      DeviceEventEmitter.addListener('stateChanged', callback);
    }
  }

  static addCurrentPlaybackTimeChangeListener(callback) {
    if (Platform.OS === 'ios') {
      const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
      tritonEmitter.addListener('currentPlaybackTimeChanged', callback);
    } else {
      DeviceEventEmitter.addListener('currentPlaybackTimeChanged', callback);
    }
  }
  //
}

export default RNTritonPlayer;
