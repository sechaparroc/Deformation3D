# Deformation3D
Implementation of Affine deformations using MLS in processing (3D).

Affine Tranformations using MLS

This is an implementation in Processing of affine transformations using the method proposed on: http://www.cs.rice.edu/~jwarren/research/mls.pdf

So, the idea is that a mesh (.obj model) is loaded and, basically, you can add, remove and traslate control points and drag them to generate their
new positions, deformating the shape of the image as Schaefer proposed.

The Scene that is shown is comopsed by two models, one is the representation of the original model, the other one is going to show the deformations
applied to the model according to the handler points.

There are two main dependences:  

- Papaya: Provides useful matrices functions. Used in this very first approach just to obtain the inverse of the affine matrix.
- ProScene: Library very useful to draw complex scenes.

In this very first approach, we use the function PointUnderPixel provides by the class Scene in order to locate a control point in
the scene, so control points are going to be added only if there's a pixel with information below the cursor.

Use the key 'c' to enable or disable the add point functionality (disable it when you want to rotate the models).
Use the key 'r' to enable or disable a bounding box over the original shape.

A next version will consider rigid transformations (Disabling transformations such as shear and keeping translation, rotation and scaling) 
and will take advantage of the info provide by the connectivity mesh using Laplacian Coordinates.
