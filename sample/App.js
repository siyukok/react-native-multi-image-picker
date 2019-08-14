/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import MultiImagePicker from "react-native-multi-image-picker";

const instructions = Platform.select({
    ios: 'Press Cmd+R to reload,\n' + 'Cmd+D or shake for dev menu',
    android:
        'Double tap R on your keyboard to reload,\n' +
        'Shake or press menu button for dev menu',
});

type Props = {};
export default class App extends Component<Props> {
    render() {
        return (
            <View style={styles.container}>
                <TouchableOpacity
                    style={{
                        width: 100,
                        height: 100,
                        backgroundColor: '#FFFFFF',
                        justifyContent: 'center',
                        alignItems: 'center'
                    }}
                    onPress={() => {
                        MultiImagePicker.openPicker({
                            // maxNum: 1,
                            // cropping: true
                        }).then(result => {
                            console.log('=============>', result);
                        }).catch(error => {
                            console.log('=============>', error);
                        });
                    }}
                >
                    <Text style={styles.welcome}>
                        open picker
                    </Text>
                </TouchableOpacity>
            </View>
        );
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5FCFF',
    },
    welcome: {
        fontSize: 20,
        textAlign: 'center',
        margin: 10,
    },
    instructions: {
        textAlign: 'center',
        color: '#333333',
        marginBottom: 5,
    },
});
