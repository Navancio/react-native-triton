declare module 'react-native-triton-player' {
  //export function configure({brand});

  export function play(tritonName, tritonMount);

  export function setNotificationStatus(status);

  export function playOnDemandStream(trackURL);

  export function pause();

  export function unPause();

  export function stop();

  export function quit();

  export function addStreamChangeListener(callback);

  export function addTrackChangeListener(callback);

  export function addStateChangeListener(callback);
}
