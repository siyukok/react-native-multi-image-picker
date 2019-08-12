import {NativeModules} from 'react-native';

const {RNMultiImagePicker} = NativeModules;

// export default RNMultiImagePicker;
export default class MultiImagePicker {
    static openPicker() {
        return RNMultiImagePicker.openPicker();
    }
}
