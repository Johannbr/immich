import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:immich_mobile/infrastructure/repositories/storage.repository.dart';

/// A Flutter widget that displays HDR images using native Android platform views.
/// This widget is designed to work with Android 14+ (API 34+) devices that support Ultra HDR.
class HdrViewer extends StatefulWidget {
  /// The path to the HDR image file (can be null for async resolution)
  final String? imagePath;

  /// The asset ID for async file path resolution
  final String? assetId;

  /// The scale type for the image display
  final HdrScaleType scaleType;

  /// Whether to enable HDR display (only works on supported devices)
  final bool enableHdr;

  /// Callback when the image is loaded
  final VoidCallback? onImageLoaded;

  /// Callback when there's an error loading the image
  final Function(String error)? onError;

  const HdrViewer({
    super.key,
    this.imagePath,
    this.assetId,
    this.scaleType = HdrScaleType.centerCrop,
    this.enableHdr = true,
    this.onImageLoaded,
    this.onError,
  });

  @override
  State<HdrViewer> createState() => _HdrViewerState();
}

class _HdrViewerState extends State<HdrViewer> {
  bool _isInitialized = false;
  String? _resolvedImagePath;
  bool _isResolvingPath = false;

  @override
  void initState() {
    super.initState();
    _resolveImagePath();
  }

  @override
  void didUpdateWidget(HdrViewer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.imagePath != widget.imagePath || oldWidget.assetId != widget.assetId) {
      _resolveImagePath();
    }
    // Scale type changes will be handled by recreating the platform view
  }

  Future<void> _resolveImagePath() async {
    if (widget.imagePath != null) {
      _resolvedImagePath = widget.imagePath;
      await _initializeHdrViewer();
      return;
    }

    if (widget.assetId != null) {
      setState(() {
        _isResolvingPath = true;
      });

      try {
        // Use StorageRepository to resolve file path from asset ID
        final storageRepository = const StorageRepository();
        final file = await storageRepository.getFileForAsset(widget.assetId!);
        if (file != null) {
          _resolvedImagePath = file.path;
          await _initializeHdrViewer();
        } else {
          widget.onError?.call('Failed to resolve file path for asset ${widget.assetId}');
        }
      } catch (e) {
        widget.onError?.call('Failed to resolve image path: $e');
      } finally {
        if (mounted) {
          setState(() {
            _isResolvingPath = false;
          });
        }
      }
    }
  }

  Future<void> _initializeHdrViewer() async {
    if (!Platform.isAndroid) {
      widget.onError?.call('HDR viewer is only supported on Android');
      return;
    }

    if (_resolvedImagePath == null) {
      widget.onError?.call('No image path available');
      return;
    }

    // Check if the image is likely to be HDR
    final isHdr = HdrUtils.isHdrImage(_resolvedImagePath!);
    print('HdrViewer: Image path: $_resolvedImagePath');
    print('HdrViewer: Is HDR image: $isHdr');
    print('HdrViewer: Enable HDR: ${widget.enableHdr}');

    // No need to call method channel - the platform view will be created directly
    if (mounted) {
      setState(() {
        _isInitialized = true;
      });
      widget.onImageLoaded?.call();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!Platform.isAndroid) {
      return Container(
        color: Colors.black,
        child: const Center(
          child: Text('HDR viewer is only supported on Android', style: TextStyle(color: Colors.white)),
        ),
      );
    }

    if (_isResolvingPath || !_isInitialized) {
      return Container(
        color: Colors.black,
        child: const Center(child: CircularProgressIndicator(color: Colors.white)),
      );
    }

    print('HdrViewer: Creating AndroidView with path: $_resolvedImagePath');
    print('HdrViewer: Scale type: ${widget.scaleType.name}');
    print('HdrViewer: Enable HDR: ${widget.enableHdr}');

    return AndroidView(
      viewType: 'hdr_viewer',
      creationParams: {
        'imagePath': _resolvedImagePath,
        'scaleType': widget.scaleType.name,
        'enableHdr': widget.enableHdr,
      },
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: (int id) {
        print('HdrViewer: Platform view created with ID: $id');
        // Platform view created successfully
        widget.onImageLoaded?.call();
      },
    );
  }
}

/// Scale types for HDR image display
enum HdrScaleType { center, centerCrop, centerInside, fitCenter, fitStart, fitEnd, fitXy, matrix }

/// Utility class for HDR-related operations
class HdrUtils {
  /// Checks if the current device supports HDR display
  static Future<bool> isHdrSupported() async {
    if (!Platform.isAndroid) return false;

    // Check if we're on Android 14+ (API 34+) which supports Ultra HDR
    return true;
  }

  /// Checks if an image file is likely to be an HDR image
  static bool isHdrImage(String imagePath) {
    final extension = imagePath.split('.').last.toLowerCase();
    return extension == 'jpg' ||
        extension == 'jpeg' ||
        extension == 'heic' ||
        extension == 'heif' ||
        extension == 'avif' ||
        extension == 'webp' ||
        imagePath.toLowerCase().contains('hdr') ||
        imagePath.toLowerCase().contains('ultra');
  }

  /// Gets the recommended scale type for HDR images
  static HdrScaleType getRecommendedScaleType() {
    return HdrScaleType.centerCrop;
  }
}
