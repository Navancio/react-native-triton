import { DeviceEventEmitter, NativeEventEmitter, NativeModules, Platform } from "react-native";

const NativeRNTritonPlayer = NativeModules.RNTritonPlayer;

class RNTritonPlayer {
	static configure({ brand }) {
		NativeRNTritonPlayer.configure(brand);
	}

	static play(tritonName, tritonMount) {
		NativeRNTritonPlayer.play(tritonName, tritonMount);
	}

	static getCurrentPlaybackTime(successCallback, errorCallback) {
		if (Platform.OS === "ios") {
			NativeRNTritonPlayer.getCurrentPlaybackTime((successValue)=> {
				//console.log('successValue', successValue);
				successCallback(successValue);
			},(errorValue) => {
				//console.log('errorValue', errorValue);
				errorCallback(errorValue);
			});
		} else {
			console.log('getCurrentPlaybackTime result', -1);
		}
	}

	static seek(offset) {
		NativeRNTritonPlayer.seek(offset);
	}

	static seekTo(offset) {
		NativeRNTritonPlayer.seekTo(offset);
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
		if (Platform.OS === "ios") {
			const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
			tritonEmitter.addListener("streamChanged", callback);
		} else {
			DeviceEventEmitter.addListener("streamChanged", callback);
		}
	}

	static addTrackChangeListener(callback) {
		if (Platform.OS === "ios") {
			const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
			tritonEmitter.addListener("trackChanged", callback);
		} else {
			DeviceEventEmitter.addListener("trackChanged", callback);
		}
	}

	static addStateChangeListener(callback) {
		if (Platform.OS === "ios") {
			const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
			tritonEmitter.addListener("stateChanged", callback);
		} else {
			DeviceEventEmitter.addListener("stateChanged", callback);
		}
	}

	static addCurrentPlaybackTimeChangeListener(callback) {
		if (Platform.OS === "ios") {
			const tritonEmitter = new NativeEventEmitter(NativeRNTritonPlayer);
			tritonEmitter.addListener("currentPlaybackTimeChanged", callback);
		} else {
			DeviceEventEmitter.addListener("currentPlaybackTimeChanged", callback);
		}
	}
	//

}

export default RNTritonPlayer;
